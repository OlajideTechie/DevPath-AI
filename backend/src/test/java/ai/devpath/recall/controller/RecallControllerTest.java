package ai.devpath.recall.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import ai.devpath.feedback.service.FeedbackProvider;

@SpringBootTest
@Transactional
class RecallControllerTest {
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
    void createsGapOffersTransferPracticeAndRequiresTwoPositiveReviewsPlusConfirmation() throws Exception {
        given(feedbackProvider.review(any(CuratedLesson.class), any()))
                .willReturn(feedback(
                        AdvisoryFeedback.ReviewRecommendation.NEEDS_REVIEW,
                        "INPUT_VALIDATION"))
                .willReturn(feedback(
                        AdvisoryFeedback.ReviewRecommendation.READY_FOR_REVIEW,
                        "INPUT_VALIDATION"))
                .willReturn(feedback(
                        AdvisoryFeedback.ReviewRecommendation.READY_FOR_REVIEW,
                        "INPUT_VALIDATION"));

        submitOriginal("""
                {"code":"class WithdrawalDraft {}"}
                """);

        mockMvc.perform(get(recallPath()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercise.lessonId").value("bank-transfer-validation-recall"))
                .andExpect(jsonPath("$.exercise.title").value("Recall: Validate a Bank Transfer"))
                .andExpect(jsonPath("$.competencies[1].competencyTag").value("INPUT_VALIDATION"))
                .andExpect(jsonPath("$.competencies[1].status").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.competencies[1].reason").value("COMPETENCY_GAP"))
                .andExpect(jsonPath("$.competencies[1].sourceAttempt.submittedCode")
                        .value("class WithdrawalDraft {}"))
                .andExpect(jsonPath("$.competencies[1].sourceAttempt.feedback.summary")
                        .value("The validation behavior was reviewed."))
                .andExpect(jsonPath("$.competencies[1].sourceAttempt.feedback.reviewRecommendation")
                        .value("NEEDS_REVIEW"));

        String recallAttempt = mockMvc.perform(post(recallPath() + "/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"competencyTag":"INPUT_VALIDATION","code":"class TransferDraft {}"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lessonId").value("bank-transfer-validation-recall"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String recallAttemptId = com.jayway.jsonpath.JsonPath.read(recallAttempt, "$.attemptId");

        mockMvc.perform(get(recallPath()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.competencies[1].status").value("PRACTICING"))
                .andExpect(jsonPath("$.competencies[1].recallReadyAttemptId").value(recallAttemptId));

        mockMvc.perform(post(recallPath() + "/INPUT_VALIDATION/demonstration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"confirmed":true}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RECALL_EVIDENCE_REQUIRED"));

        String baseAttempt = mockMvc.perform(post(originalSubmissionPath())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"class ImprovedWithdrawalDraft {}"}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String baseAttemptId = com.jayway.jsonpath.JsonPath.read(baseAttempt, "$.attemptId");

        mockMvc.perform(post(recallPath() + "/INPUT_VALIDATION/demonstration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"confirmed":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEMONSTRATED"))
                .andExpect(jsonPath("$.baseReadyAttemptId").value(baseAttemptId))
                .andExpect(jsonPath("$.recallReadyAttemptId").value(recallAttemptId))
                .andExpect(jsonPath("$.learnerConfirmed").value(true));

        mockMvc.perform(get(recallPath()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.competencies[1].status").value("DEMONSTRATED"));
    }

    @Test
    void doesNotCreateRecallForOptionalImprovementsOrBroadCompetencyTags() throws Exception {
        given(feedbackProvider.review(any(CuratedLesson.class), any()))
                .willReturn(new AdvisoryFeedback(
                        "The exercise requirements are satisfied.",
                        List.of("Uses the required checks."),
                        List.of(),
                        List.of("Consider a clearer local variable name."),
                        List.of("INPUT_VALIDATION", "NULL_HANDLING"),
                        "Continue to the next lesson; this naming suggestion is optional.",
                        AdvisoryFeedback.ReviewRecommendation.CONTINUE_PRACTICE,
                        List.of(new FeedbackFinding(
                                FeedbackFinding.Category.OPTIONAL_IMPROVEMENT,
                                FeedbackFinding.Competency.INPUT_VALIDATION,
                                "A clearer local variable name is optional.")),
                        AssessmentConfidence.HIGH,
                        NextActionType.NEXT_LESSON));

        submitOriginal("""
                {"code":"class CompleteWithdrawalValidator {}"}
                """);

        mockMvc.perform(get(recallPath()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.competencies[1].status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.competencies[1].reason").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.competencies[4].status").value("NOT_STARTED"));
    }

    @Test
    void recordsExplicitPlannedReinforcementWithoutCallingItACompetencyGap() throws Exception {
        given(feedbackProvider.review(any(CuratedLesson.class), any()))
                .willReturn(new AdvisoryFeedback(
                        "The requirements are satisfied; extra practice is suggested.",
                        List.of("Uses BigDecimal comparisons."),
                        List.of(),
                        List.of(),
                        List.of("BOUNDARY_CONDITIONS"),
                        "Try the transfer exercise to reinforce the boundary.",
                        AdvisoryFeedback.ReviewRecommendation.CONTINUE_PRACTICE,
                        List.of(new FeedbackFinding(
                                FeedbackFinding.Category.PLANNED_REINFORCEMENT,
                                FeedbackFinding.Competency.BOUNDARY_CONDITIONS,
                                "Practise the exact-balance boundary in a transfer context.")),
                        AssessmentConfidence.HIGH,
                        NextActionType.REINFORCEMENT));

        submitOriginal("""
                {"code":"class CompleteWithdrawalValidator {}"}
                """);

        mockMvc.perform(get(recallPath()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.competencies[0].competencyTag").value("JAVA_METHODS"))
                .andExpect(jsonPath("$.competencies[0].status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.competencies[2].competencyTag").value("BOUNDARY_CONDITIONS"))
                .andExpect(jsonPath("$.competencies[2].status").value("PRACTICING"))
                .andExpect(jsonPath("$.competencies[2].reason").value("PLANNED_REINFORCEMENT"));
    }

    @Test
    void returnsNotStartedStatusesAndValidatesRecallSubmission() throws Exception {
        mockMvc.perform(get(recallPath()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.competencies[?(@.status=='NOT_STARTED')]").isNotEmpty());

        mockMvc.perform(post(recallPath() + "/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"competencyTag":"UNSUPPORTED","code":"class Draft {}"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post(recallPath() + "/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"competencyTag":"INPUT_VALIDATION","code":"class Draft {}"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECALL_NOT_FOUND"));

        mockMvc.perform(post(recallPath() + "/INPUT_VALIDATION/demonstration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/learners/{learnerId}/recall", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEARNER_NOT_FOUND"));
        org.mockito.Mockito.verifyNoInteractions(feedbackProvider);
    }

    private void submitOriginal(String body) throws Exception {
        mockMvc.perform(post(originalSubmissionPath())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private AdvisoryFeedback feedback(
            AdvisoryFeedback.ReviewRecommendation recommendation,
            String competencyTag) {
        List<FeedbackFinding> findings = recommendation == AdvisoryFeedback.ReviewRecommendation.NEEDS_REVIEW
                ? List.of(new FeedbackFinding(
                        FeedbackFinding.Category.FUNCTIONAL_DEFECT,
                        FeedbackFinding.Competency.valueOf(competencyTag),
                        "The implementation misses a required boundary."))
                : List.of();
        return new AdvisoryFeedback(
                "The validation behavior was reviewed.",
                List.of("Uses focused methods."),
                List.of("Review the amount boundaries."),
                List.of("Check every input before comparing."),
                List.of(competencyTag),
                "Review the transfer boundary cases.",
                recommendation,
                findings,
                AssessmentConfidence.HIGH,
                recommendation == AdvisoryFeedback.ReviewRecommendation.NEEDS_REVIEW
                        ? NextActionType.TARGETED_CORRECTION
                        : NextActionType.NEXT_LESSON);
    }

    private String recallPath() {
        return "/api/v1/learners/" + learnerId + "/recall";
    }

    private String originalSubmissionPath() {
        return "/api/v1/learners/" + learnerId
                + "/lessons/java-methods-input-validation/submissions";
    }
}
