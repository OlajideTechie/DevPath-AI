package ai.devpath.progress.dto;

public record NextActivityResponse(
        ProgressActivityType type,
        String lessonId,
        String title,
        int estimatedMinutes,
        String reason,
        String competencyTag) {
}
