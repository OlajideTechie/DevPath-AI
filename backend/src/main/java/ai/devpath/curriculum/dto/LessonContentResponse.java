package ai.devpath.curriculum.dto;

import java.util.List;

import ai.devpath.curriculum.catalog.CuratedLesson;

public record LessonContentResponse(
        String lessonId,
        String title,
        String summary,
        String learningObjective,
        int estimatedMinutes,
        String qaContext,
        List<CuratedLesson.LessonSection> sections,
        CuratedLesson.PracticeExercise exercise) {
}
