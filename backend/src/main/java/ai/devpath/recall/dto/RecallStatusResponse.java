package ai.devpath.recall.dto;

import java.util.UUID;

import ai.devpath.recall.model.RecallStatus;

public record RecallStatusResponse(
        String competencyTag,
        RecallStatus status,
        UUID baseReadyAttemptId,
        UUID recallReadyAttemptId,
        boolean learnerConfirmed) {
}
