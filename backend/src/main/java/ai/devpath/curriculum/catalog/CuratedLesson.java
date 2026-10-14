package ai.devpath.curriculum.catalog;

import java.util.List;

public record CuratedLesson(
        String lessonId,
        String title,
        String summary,
        String learningObjective,
        int estimatedMinutes,
        String qaContext,
        List<LessonSection> sections,
        PracticeExercise exercise) {
    public record LessonSection(String heading, String content, String codeExample) {
    }

    public record PracticeExercise(
            String title,
            String prompt,
            List<String> requirements,
            List<String> testCases,
            String starterCode) {
    }
}
