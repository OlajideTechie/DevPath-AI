package ai.devpath.progress.dto;

import java.util.List;
import java.util.UUID;

public record ProgressResponse(
        UUID learnerId,
        long lessonsAttempted,
        long exercisesSubmitted,
        List<ProgressCompetencyResponse> competenciesNeedingReview,
        List<ProgressCompetencyResponse> competenciesInProgress,
        List<ProgressCompetencyResponse> competenciesDemonstrated,
        LatestFeedbackResponse latestFeedback,
        NextActivityResponse nextActivity) {
}
