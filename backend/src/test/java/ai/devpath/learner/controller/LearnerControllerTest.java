package ai.devpath.learner.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import ai.devpath.learner.model.Learner;
import ai.devpath.learner.model.StudyDay;
import ai.devpath.learner.repository.LearnerRepository;

@SpringBootTest
@Transactional
class LearnerControllerTest {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private LearnerRepository learnerRepository;

    @Test
    void createsLearnerWithServerControlledFieldsAndLocation() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        var result = mockMvc.perform(post("/api/v1/learners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentRole": "Client supplied role",
                                  "experienceBand": "FOUR_TO_FIVE",
                                  "specializations": ["API Testing", "Database Testing"],
                                  "existingSkills": [" Postman ", "SQL", "Postman", "sql"],
                                  "programmingLevel": "BEGINNER",
                                  "studyDays": ["MONDAY", "WEDNESDAY", "SATURDAY"],
                                  "dailyMinutes": 45,
                                  "learningStyle": "PRACTICAL",
                                  "targetRole": "Client supplied value",
                                  "preferredLanguage": "CLIENT_VALUE",
                                  "profileStatus": "CLIENT_VALUE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.currentRole").value("QA Engineer"))
                .andExpect(jsonPath("$.targetRole").value("Java Backend Developer"))
                .andExpect(jsonPath("$.preferredLanguage").value("JAVA"))
                .andExpect(jsonPath("$.programmingLevel").value("BEGINNER"))
                .andExpect(jsonPath("$.profileStatus").value("CREATED"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.message").doesNotExist());

        String location = result.andReturn().getResponse().getHeader("Location");
        assertThat(location).matches("/api/v1/learners/[0-9a-f-]{36}");
        Learner learner = learnerRepository.findById(
                UUID.fromString(location.substring(location.lastIndexOf('/') + 1))).orElseThrow();
        assertThat(learner.getExistingSkills()).containsExactly("Postman", "SQL");
        assertThat(learner.getCurrentRole()).isEqualTo("QA Engineer");
        assertThat(learner.getExperienceBand().name()).isEqualTo("FOUR_TO_FIVE");
        assertThat(learner.getSpecializations()).containsExactly("API Testing", "Database Testing");
        assertThat(learner.getYearsOfExperience()).isEqualTo(4);
        assertThat(learner.getStudyDays()).containsExactly(
                StudyDay.MONDAY,
                StudyDay.WEDNESDAY,
                StudyDay.SATURDAY);
        assertThat(learner.getTargetRole()).isEqualTo("Java Backend Developer");
        assertThat(learner.getPreferredLanguage().name()).isEqualTo("JAVA");
        assertThat(learner.getProfileStatus().name()).isEqualTo("CREATED");
    }

    @Test
    void getsPersistedLearnerProfileWithAllContractFields() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        var createResult = mockMvc.perform(post("/api/v1/learners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "experienceBand": "FOUR_TO_FIVE",
                                  "specializations": ["API Testing"],
                                  "existingSkills": ["Postman", "SQL"],
                                  "programmingLevel": "INTERMEDIATE",
                                  "studyDays": ["MONDAY", "WEDNESDAY", "SATURDAY"],
                                  "dailyMinutes": 45,
                                  "learningStyle": "PRACTICAL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String location = createResult.getResponse().getHeader("Location");
        Learner learner = learnerRepository.findById(
                UUID.fromString(location.substring(location.lastIndexOf('/') + 1))).orElseThrow();

        mockMvc.perform(get("/api/v1/learners/{learnerId}", learner.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(learner.getId().toString()))
                .andExpect(jsonPath("$.currentRole").value("QA Engineer"))
                .andExpect(jsonPath("$.experienceBand").value("FOUR_TO_FIVE"))
                .andExpect(jsonPath("$.specializations[0]").value("API Testing"))
                .andExpect(jsonPath("$.existingSkills").isArray())
                .andExpect(jsonPath("$.existingSkills[0]").value("Postman"))
                .andExpect(jsonPath("$.existingSkills[1]").value("SQL"))
                .andExpect(jsonPath("$.targetRole").value("Java Backend Developer"))
                .andExpect(jsonPath("$.preferredLanguage").value("JAVA"))
                .andExpect(jsonPath("$.programmingLevel").value("INTERMEDIATE"))
                .andExpect(jsonPath("$.studyDays[0]").value("MONDAY"))
                .andExpect(jsonPath("$.studyDays[1]").value("WEDNESDAY"))
                .andExpect(jsonPath("$.studyDays[2]").value("SATURDAY"))
                .andExpect(jsonPath("$.dailyMinutes").value(45))
                .andExpect(jsonPath("$.learningStyle").value("PRACTICAL"))
                .andExpect(jsonPath("$.profileStatus").value("CREATED"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").doesNotExist());
    }

    @Test
    void returnsNotFoundEnvelopeForUnknownLearner() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        mockMvc.perform(get("/api/v1/learners/{learnerId}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEARNER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Learner not found"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void returnsBadRequestForMalformedLearnerUuid() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        mockMvc.perform(get("/api/v1/learners/{learnerId}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid learner profile"))
                .andExpect(jsonPath("$.fieldErrors.learnerId").value("must be a valid UUID"));
    }

    @Test
    void rejectsDailyMinutesOutsideContractRangeWithValidationEnvelope() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        long initialCount = learnerRepository.count();
        mockMvc.perform(post("/api/v1/learners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(241)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid learner profile"))
                .andExpect(jsonPath("$.fieldErrors.dailyMinutes").value("must be between 10 and 240"));

        assertThat(learnerRepository.count()).isEqualTo(initialCount);
    }

    @Test
    void rejectsDuplicateStudyDaysWithValidationEnvelope() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        long initialCount = learnerRepository.count();
        mockMvc.perform(post("/api/v1/learners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "experienceBand": "FOUR_TO_FIVE",
                                  "specializations": ["API Testing"],
                                  "existingSkills": ["SQL"],
                                  "programmingLevel": "BEGINNER",
                                  "studyDays": ["MONDAY", "MONDAY"],
                                  "dailyMinutes": 45,
                                  "learningStyle": "PRACTICAL"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid learner profile"))
                .andExpect(jsonPath("$.fieldErrors.studyDays").exists());

        assertThat(learnerRepository.count()).isEqualTo(initialCount);
    }

    @Test
    void rejectsLearnerWithoutQaSpecialization() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        long initialCount = learnerRepository.count();
        mockMvc.perform(post("/api/v1/learners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "experienceBand": "FOUR_TO_FIVE",
                                  "specializations": [],
                                  "existingSkills": [],
                                  "programmingLevel": "BEGINNER",
                                  "studyDays": ["MONDAY"],
                                  "dailyMinutes": 45,
                                  "learningStyle": "PRACTICAL"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.specializations").exists());

        assertThat(learnerRepository.count()).isEqualTo(initialCount);
    }

    @Test
    void rejectsUnknownEnumValuesWithValidationEnvelope() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        long initialCount = learnerRepository.count();
        mockMvc.perform(post("/api/v1/learners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "experienceBand": "UNKNOWN",
                                  "specializations": ["API Testing"],
                                  "existingSkills": ["SQL"],
                                  "programmingLevel": "NOVICE",
                                  "studyDays": ["MONDAY"],
                                  "dailyMinutes": 45,
                                  "learningStyle": "PRACTICAL"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid learner profile"))
                .andExpect(jsonPath("$.fieldErrors.request").value("Malformed JSON request"));

        assertThat(learnerRepository.count()).isEqualTo(initialCount);
    }

    private String validRequest(int dailyMinutes) {
        return """
                {
                  "experienceBand": "FOUR_TO_FIVE",
                  "specializations": ["API Testing"],
                  "existingSkills": ["SQL"],
                  "programmingLevel": "BEGINNER",
                  "studyDays": ["MONDAY", "WEDNESDAY"],
                  "dailyMinutes": %d,
                  "learningStyle": "PRACTICAL"
                }
                """.formatted(dailyMinutes);
    }
}
