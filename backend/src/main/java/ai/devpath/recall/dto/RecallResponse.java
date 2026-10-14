package ai.devpath.recall.dto;

import java.util.List;
import java.util.UUID;

public record RecallResponse(
        UUID learnerId,
        RecallExerciseResponse exercise,
        List<CompetencyRecallResponse> competencies,
        String assessmentPolicy) {
}
