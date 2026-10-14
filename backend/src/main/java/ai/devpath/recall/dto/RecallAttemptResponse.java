package ai.devpath.recall.dto;

import java.time.Instant;
import java.util.UUID;

import ai.devpath.feedback.dto.AdvisoryFeedback;

public record RecallAttemptResponse(
        UUID attemptId,
        String lessonId,
        String submittedCode,
        AdvisoryFeedback feedback,
        Instant submittedAt) {
}
