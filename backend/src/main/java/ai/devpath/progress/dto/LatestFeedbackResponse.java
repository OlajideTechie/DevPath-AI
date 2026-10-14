package ai.devpath.progress.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ai.devpath.feedback.dto.AdvisoryFeedback;
import ai.devpath.feedback.dto.FeedbackSource;

public record LatestFeedbackResponse(
        UUID attemptId,
        String lessonId,
        Instant submittedAt,
        String summary,
        String nextAction,
        List<String> competencyTags,
        AdvisoryFeedback.ReviewRecommendation reviewRecommendation,
        FeedbackSource feedbackSource) {
}
