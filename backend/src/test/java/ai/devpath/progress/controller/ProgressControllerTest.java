package ai.devpath.progress.controller;

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
import ai.devpath.feedback.service.FeedbackProviderException;

@SpringBootTest
@Transactional
class ProgressControllerTest {
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
    void reportsPersistedCountsCompetenciesLatestFeedbackAndNextActivity() throws Exception {
        given(feedbackProvider.review(any(CuratedLesson.class), any()))
                .willReturn(feedback(
                        "Initial review needs more work.",
                        AdvisoryFeedback.ReviewRecommendation.NEEDS_REVIEW,
                        "INPUT_VALIDATION",
                        "NULL_HANDLING"))
                .willReturn(feedback(
                        "The original exercise is ready.",
                        AdvisoryFeedback.ReviewRecommendation.READY_FOR_REVIEW,
                        "INPUT_VALIDATION"))
                .willReturn(feedback(
                        "The transfer exercise is ready.",
                        AdvisoryFeedback.ReviewRecommendation.READY_FOR_REVIEW,
                        "INPUT_VALIDATION"));

        submit("/lessons/java-methods-input-validation/submissions", """
                {"code":"class FirstDraft {}"}
                """);
        submit("/lessons/java-methods-input-validation/submissions", """
                {"code":"class ImprovedDraft {}"}
                """);
        submit("/recall/submissions", """
                {"competencyTag":"INPUT_VALIDATION","code":"class TransferDraft {}"}
                """);
        mockMvc.perform(post("/api/v1/learners/" + learnerId
                        + "/recall/INPUT_VALIDATION/demonstration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"confirmed":true}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get(progressPath()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learnerId").value(learnerId.toString()))
                .andExpect(jsonPath("$.lessonsAttempted").value(2))
                .andExpect(jsonPath("$.exercisesSubmitted").value(3))
                .andExpect(jsonPath("$.competenciesNeedingReview.length()").value(1))
                .andExpect(jsonPath("$.competenciesNeedingReview[0].competencyTag").value("NULL_HANDLING"))
                .andExpect(jsonPath("$.competenciesNeedingReview[0].status").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.competenciesInProgress").isEmpty())
                .andExpect(jsonPath("$.competenciesDemonstrated.length()").value(1))
                .andExpect(jsonPath("$.competenciesDemonstrated[0].competencyTag").value("INPUT_VALIDATION"))
                .andExpect(jsonPath("$.latestFeedback.lessonId").value("bank-transfer-validation-recall"))
                .andExpect(jsonPath("$.latestFeedback.summary").value("The transfer exercise is ready."))
                .andExpect(jsonPath("$.latestFeedback.reviewRecommendation").value("READY_FOR_REVIEW"))
                .andExpect(jsonPath("$.nextActivity.type").value("RECALL"))
                .andExpect(jsonPath("$.nextActivity.competencyTag").value("NULL_HANDLING"));
    }

    @Test
    void reportsEmptyProgressAndCountsAttemptsWhenFeedbackIsUnavailable() throws Exception {
        mockMvc.perform(get(progressPath()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonsAttempted").value(0))
                .andExpect(jsonPath("$.exercisesSubmitted").value(0))
                .andExpect(jsonPath("$.competenciesNeedingReview").isEmpty())
                .andExpect(jsonPath("$.competenciesInProgress").isEmpty())
                .andExpect(jsonPath("$.competenciesDemonstrated").isEmpty())
                .andExpect(jsonPath("$.latestFeedback").doesNotExist())
                .andExpect(jsonPath("$.nextActivity.type").value("LESSON"))
                .andExpect(jsonPath("$.nextActivity.lessonId").value("java-methods-input-validation"));

        given(feedbackProvider.review(any(CuratedLesson.class), any()))
                .willThrow(new FeedbackProviderException("provider unavailable"));
        submit("/lessons/java-methods-input-validation/submissions", """
                {"code":"class Draft {}"}
                """, 503);

        mockMvc.perform(get(progressPath()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonsAttempted").value(1))
                .andExpect(jsonPath("$.exercisesSubmitted").value(1))
                .andExpect(jsonPath("$.latestFeedback").doesNotExist());
    }

    @Test
    void recommendsTheOriginalLessonWhenTransferIsReadyButBaseEvidenceIsMissing() throws Exception {
        given(feedbackProvider.review(any(CuratedLesson.class), any()))
                .willReturn(feedback(
                        "The competency needs more review.",
                        AdvisoryFeedback.ReviewRecommendation.NEEDS_REVIEW,
                        "INPUT_VALIDATION"))
                .willReturn(feedback(
                        "The transfer exercise is ready.",
                        AdvisoryFeedback.ReviewRecommendation.READY_FOR_REVIEW,
                        "INPUT_VALIDATION"));

        submit("/lessons/java-methods-input-validation/submissions", """
                {"code":"class WithdrawalDraft {}"}
                """);
        submit("/recall/submissions", """
                {"competencyTag":"INPUT_VALIDATION","code":"class TransferDraft {}"}
                """);

        mockMvc.perform(get(progressPath()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.competenciesInProgress.length()").value(1))
                .andExpect(jsonPath("$.competenciesInProgress[0].competencyTag").value("INPUT_VALIDATION"))
                .andExpect(jsonPath("$.competenciesInProgress[0].status").value("PRACTICING"))
                .andExpect(jsonPath("$.nextActivity.type").value("LESSON"))
                .andExpect(jsonPath("$.nextActivity.lessonId").value("java-methods-input-validation"))
                .andExpect(jsonPath("$.nextActivity.competencyTag").doesNotExist());
    }

    @Test
    void returnsNotFoundForUnknownLearner() throws Exception {
        mockMvc.perform(get("/api/v1/learners/{learnerId}/progress", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEARNER_NOT_FOUND"));
    }

    @Test
    void returnsBadRequestForMalformedLearnerId() throws Exception {
        mockMvc.perform(get("/api/v1/learners/not-a-uuid/progress"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private void submit(String path, String body) throws Exception {
        submit(path, body, 201);
    }

    private void submit(String path, String body, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/v1/learners/" + learnerId + path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(expectedStatus));
    }

    private AdvisoryFeedback feedback(
            String summary,
            AdvisoryFeedback.ReviewRecommendation recommendation,
            String... competencyTags) {
        List<FeedbackFinding> findings = recommendation == AdvisoryFeedback.ReviewRecommendation.NEEDS_REVIEW
                ? java.util.Arrays.stream(competencyTags)
                        .map(tag -> new FeedbackFinding(
                                FeedbackFinding.Category.FUNCTIONAL_DEFECT,
                                FeedbackFinding.Competency.valueOf(tag),
                                "A required exercise case is not handled."))
                        .toList()
                : List.of();
        return new AdvisoryFeedback(
                summary,
                List.of("Uses a focused validation method."),
                List.of("Review edge cases."),
                List.of("Check all values before comparison."),
                List.of(competencyTags),
                "Review the persisted feedback and next action.",
                recommendation,
                findings,
                AssessmentConfidence.HIGH,
                recommendation == AdvisoryFeedback.ReviewRecommendation.NEEDS_REVIEW
                        ? NextActionType.TARGETED_CORRECTION
                        : recommendation == AdvisoryFeedback.ReviewRecommendation.READY_FOR_REVIEW
                                ? NextActionType.NEXT_LESSON
                                : NextActionType.REINFORCEMENT);
    }

    private String progressPath() {
        return "/api/v1/learners/" + learnerId + "/progress";
    }
}
