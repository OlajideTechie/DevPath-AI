package ai.devpath.recall.dto;

import java.time.Instant;
import java.util.UUID;

import ai.devpath.recall.model.RecallReason;
import ai.devpath.recall.model.RecallStatus;

public record CompetencyRecallResponse(
        String competencyTag,
        RecallStatus status,
        UUID gapAttemptId,
        UUID baseReadyAttemptId,
        UUID recallReadyAttemptId,
        boolean learnerConfirmed,
        Instant updatedAt,
        RecallReason reason,
        RecallAttemptResponse sourceAttempt) {
}
