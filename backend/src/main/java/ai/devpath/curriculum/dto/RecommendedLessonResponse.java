package ai.devpath.curriculum.dto;

public record RecommendedLessonResponse(
        String lessonId,
        String title,
        String summary,
        String learningObjective,
        int estimatedMinutes,
        String recommendationReason,
        String qaScenario) {
}
