package ai.devpath.progress.dto;

import java.time.Instant;

import ai.devpath.recall.model.RecallStatus;

public record ProgressCompetencyResponse(
        String competencyTag,
        RecallStatus status,
        Instant updatedAt) {
}
