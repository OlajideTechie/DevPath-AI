package ai.devpath.feedback.dto;

import java.time.Instant;
import java.util.UUID;

public record SubmissionResponse(
        UUID attemptId,
        UUID learnerId,
        String lessonId,
        AdvisoryFeedback feedback,
        boolean advisoryOnly,
        boolean codeExecutionPerformed,
        Instant submittedAt) {
}
