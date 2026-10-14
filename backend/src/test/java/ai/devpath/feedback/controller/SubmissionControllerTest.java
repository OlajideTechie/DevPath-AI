package ai.devpath.feedback.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import ai.devpath.curriculum.catalog.CuratedLesson;
import ai.devpath.feedback.dto.AdvisoryFeedback;
import ai.devpath.feedback.dto.AssessmentConfidence;
import ai.devpath.feedback.dto.FeedbackFinding;
import ai.devpath.feedback.dto.NextActionType;
import ai.devpath.feedback.model.AttemptStatus;
import ai.devpath.feedback.repository.LearningAttemptRepository;
import ai.devpath.feedback.service.FeedbackProvider;
import ai.devpath.feedback.service.FeedbackProviderException;

@SpringBootTest
@Transactional
class SubmissionControllerTest {
    private static final String LEARNER_REQUEST = """
            {
              "experienceBand": "FOUR_TO_FIVE",
              "specializations": ["API Testing"],
              "existingSkills": [],
              "programmingLevel": "BEGINNER",
              "studyDays": ["MONDAY"],
              "dailyMinutes": 30,
              "learningStyle": "PRACTICAL"
            }
            """;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private LearningAttemptRepository attemptRepository;

    @MockitoBean
    private FeedbackProvider feedbackProvider;

    private MockMvc mockMvc;
    private UUID learnerId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        var createResult = mockMvc.perform(post("/api/v1/learners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LEARNER_REQUEST))
                .andExpect(status().isCreated())
                .andReturn();
        String location = createResult.getResponse().getHeader("Location");
        learnerId = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }

    @Test
    void submitsCodeGetsStructuredAdvisoryFeedbackAndPersistsAttempt() throws Exception {
        AdvisoryFeedback feedback = validFeedback();
        given(feedbackProvider.review(any(CuratedLesson.class), eq("public class Draft {}")))
                .willReturn(feedback);

        var result = mockMvc.perform(post(submissionPath())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"public class Draft {}"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attemptId").isNotEmpty())
                .andExpect(jsonPath("$.learnerId").value(learnerId.toString()))
                .andExpect(jsonPath("$.lessonId").value("java-methods-input-validation"))
                .andExpect(jsonPath("$.advisoryOnly").value(true))
                .andExpect(jsonPath("$.codeExecutionPerformed").value(false))
                .andExpect(jsonPath("$.feedback.summary").value(feedback.summary()))
                .andExpect(jsonPath("$.feedback.strengths[0]").value("Uses BigDecimal"))
                .andExpect(jsonPath("$.feedback.issues[0]").value("Null inputs are not checked"))
                .andExpect(jsonPath("$.feedback.improvements[0]").value("Guard both values first"))
                .andExpect(jsonPath("$.feedback.competencyTags[0]").value("INPUT_VALIDATION"))
                .andExpect(jsonPath("$.feedback.nextAction").value("Add null and boundary guards"))
                .andExpect(jsonPath("$.feedback.reviewRecommendation").value("NEEDS_REVIEW"))
                .andReturn();

        String attemptId = com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.attemptId");
        var attempt = attemptRepository.findById(UUID.fromString(attemptId)).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(attempt.getLearner().getId()).isEqualTo(learnerId);
        org.assertj.core.api.Assertions.assertThat(attempt.getLessonId())
                .isEqualTo("java-methods-input-validation");
        org.assertj.core.api.Assertions.assertThat(attempt.getSubmittedCode())
                .isEqualTo("public class Draft {}");
        org.assertj.core.api.Assertions.assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.FEEDBACK_READY);
        org.assertj.core.api.Assertions.assertThat(attempt.getFeedback().path("summary").asText())
                .isEqualTo(feedback.summary());
    }

    @Test
    void recordsAttemptAndReturnsControlledErrorWhenProviderFails() throws Exception {
        given(feedbackProvider.review(any(CuratedLesson.class), any()))
                .willThrow(new FeedbackProviderException("provider details must not leak"));

        mockMvc.perform(post(submissionPath())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"class Draft {}"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_FEEDBACK_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value(
                        "Advisory feedback is temporarily unavailable. Your attempt was recorded; please retry."));

        List<ai.devpath.feedback.model.LearningAttempt> attempts = attemptRepository.findAll();
        org.assertj.core.api.Assertions.assertThat(attempts)
                .filteredOn(attempt -> attempt.getLearner().getId().equals(learnerId))
                .singleElement()
                .satisfies(attempt -> {
                    org.assertj.core.api.Assertions.assertThat(attempt.getStatus())
                            .isEqualTo(AttemptStatus.FEEDBACK_FAILED);
                    org.assertj.core.api.Assertions.assertThat(attempt.getFeedback()).isNull();
                });
    }

    @Test
    void rejectsBlankOversizedAndUnknownLessonSubmissionsBeforeProviderCall() throws Exception {
        mockMvc.perform(post(submissionPath())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        String oversizedCode = "x".repeat(20001);
        mockMvc.perform(post(submissionPath())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                                .writeValueAsString(java.util.Map.of("code", oversizedCode))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/v1/learners/{learnerId}/lessons/{lessonId}/submissions", learnerId, "other")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"class Draft {}"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LESSON_NOT_FOUND"));
        org.mockito.Mockito.verifyNoInteractions(feedbackProvider);
        org.assertj.core.api.Assertions.assertThat(attemptRepository.findAll())
                .noneMatch(attempt -> attempt.getLearner().getId().equals(learnerId));
    }

    @Test
    void returnsNotFoundWhenLearnerDoesNotExist() throws Exception {
        mockMvc.perform(post(
                                "/api/v1/learners/{learnerId}/lessons/{lessonId}/submissions",
                                UUID.randomUUID(),
                                "java-methods-input-validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"class Draft {}"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEARNER_NOT_FOUND"));
        org.mockito.Mockito.verifyNoInteractions(feedbackProvider);
    }

    private String submissionPath() {
        return "/api/v1/learners/" + learnerId
                + "/lessons/java-methods-input-validation/submissions";
    }

    private AdvisoryFeedback validFeedback() {
        return new AdvisoryFeedback(
                "The method is started but needs defensive validation.",
                List.of("Uses BigDecimal"),
                List.of("Null inputs are not checked"),
                List.of("Guard both values first"),
                List.of("INPUT_VALIDATION", "NULL_HANDLING"),
                "Add null and boundary guards",
                AdvisoryFeedback.ReviewRecommendation.NEEDS_REVIEW,
                List.of(new FeedbackFinding(
                        FeedbackFinding.Category.FUNCTIONAL_DEFECT,
                        FeedbackFinding.Competency.INPUT_VALIDATION,
                        "Null arguments need to return false.")),
                AssessmentConfidence.HIGH,
                NextActionType.TARGETED_CORRECTION);
    }
}
