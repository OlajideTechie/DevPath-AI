package ai.devpath.curriculum.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class LessonRecommendationControllerTest {
    @Autowired
    private WebApplicationContext context;

    @Test
    void recommendsCuratedStarterAndAdaptsTheQaExampleToProfile() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        var createResult = mockMvc.perform(post("/api/v1/learners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "experienceBand": "FOUR_TO_FIVE",
                                  "specializations": ["API Testing"],
                                  "existingSkills": [],
                                  "programmingLevel": "BEGINNER",
                                  "studyDays": ["MONDAY"],
                                  "dailyMinutes": 30,
                                  "learningStyle": "PRACTICAL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String location = createResult.getResponse().getHeader("Location");
        String learnerId = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(get("/api/v1/learners/{learnerId}/recommended-lesson", learnerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonId").value("java-methods-input-validation"))
                .andExpect(jsonPath("$.title").value("Java Methods and Input Validation"))
                .andExpect(jsonPath("$.estimatedMinutes").value(20))
                .andExpect(jsonPath("$.recommendationReason").value(
                        "Start with one focused Java method: identify its inputs, check for missing or non-positive values, then compare the amount."))
                .andExpect(jsonPath("$.qaScenario").value(startsWith(
                        "Build a focused regression set for null, non-positive, exact-balance, and over-balance requests.")))
                .andExpect(jsonPath("$.qaScenario").value(endsWith(
                        "Apply your API testing experience to check the withdrawal endpoint's accepted and rejected responses.")));
    }

    @ParameterizedTest
    @CsvSource({
        "ZERO_TO_ONE, 'Start with three straightforward cases:'",
        "TWO_TO_THREE, 'Try valid and invalid requests, including the exact-balance boundary.'",
        "FOUR_TO_FIVE, 'Build a focused regression set for null, non-positive, exact-balance, and over-balance requests.'",
        "SIX_TO_EIGHT, 'Design boundary-focused coverage across valid, malformed, and over-balance requests, and verify rejected requests do not change the balance.'",
        "NINE_PLUS, 'Define a layered contract and regression strategy for input partitions, monetary boundaries, rejection semantics, and account-state invariants.'"
    })
    void adaptsQaExampleComplexityToPersistedExperienceBand(
            String experienceBand,
            String expectedGuidance) throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        var createResult = mockMvc.perform(post("/api/v1/learners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "experienceBand": "%s",
                                  "specializations": ["API Testing"],
                                  "existingSkills": [],
                                  "programmingLevel": "BEGINNER",
                                  "studyDays": ["MONDAY"],
                                  "dailyMinutes": 30,
                                  "learningStyle": "PRACTICAL"
                                }
                                """.formatted(experienceBand)))
                .andExpect(status().isCreated())
                .andReturn();
        String location = createResult.getResponse().getHeader("Location");
        String learnerId = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(get("/api/v1/learners/{learnerId}/recommended-lesson", learnerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qaScenario").value(startsWith(expectedGuidance)));
    }

    @ParameterizedTest
    @CsvSource({
        "BEGINNER, 'Start with one focused Java method:'",
        "INTERMEDIATE, 'Trace how guard clauses validate inputs before business-rule comparisons,'",
        "EXPERIENCED, 'Review method boundaries, null safety, and BigDecimal value semantics'"
    })
    void adaptsJavaFocusToDeclaredProgrammingLevel(
            String programmingLevel,
            String expectedFocus) throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        var createResult = mockMvc.perform(post("/api/v1/learners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "experienceBand": "ZERO_TO_ONE",
                                  "specializations": ["API Testing"],
                                  "existingSkills": [],
                                  "programmingLevel": "%s",
                                  "studyDays": ["MONDAY"],
                                  "dailyMinutes": 30,
                                  "learningStyle": "PRACTICAL"
                                }
                                """.formatted(programmingLevel)))
                .andExpect(status().isCreated())
                .andReturn();
        String location = createResult.getResponse().getHeader("Location");
        String learnerId = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(get("/api/v1/learners/{learnerId}/recommended-lesson", learnerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendationReason").value(startsWith(expectedFocus)));
    }

    @Test
    void returnsNotFoundEnvelopeWhenLearnerDoesNotExist() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        mockMvc.perform(get("/api/v1/learners/{learnerId}/recommended-lesson", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEARNER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Learner not found"));
    }

    @Test
    void returnsNotFoundEnvelopeForUnknownLesson() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        mockMvc.perform(get("/api/v1/lessons/{lessonId}", "missing-lesson"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LESSON_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Lesson not found"));
    }

    @Test
    void returnsCuratedLessonSectionsAndExercise() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        mockMvc.perform(get("/api/v1/lessons/{lessonId}", "java-methods-input-validation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonId").value("java-methods-input-validation"))
                .andExpect(jsonPath("$.title").value("Java Methods and Input Validation"))
                .andExpect(jsonPath("$.qaContext").value(
                        "A banking API receives withdrawal requests. Validate each request before any balance update, just as you would verify API boundary cases in a QA test."))
                .andExpect(jsonPath("$.sections.length()").value(3))
                .andExpect(jsonPath("$.sections[0].heading").value("A method gives a rule a clear boundary"))
                .andExpect(jsonPath("$.exercise.title").value("Validate a banking withdrawal"))
                .andExpect(jsonPath("$.exercise.requirements.length()").value(6))
                .andExpect(jsonPath("$.exercise.testCases.length()").value(6))
                .andExpect(jsonPath("$.exercise.starterCode").value(org.hamcrest.Matchers.containsString("BigDecimal")));
    }
}
