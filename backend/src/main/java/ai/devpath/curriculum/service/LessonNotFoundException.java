package ai.devpath.curriculum.service;

public class LessonNotFoundException extends RuntimeException {
    public LessonNotFoundException(String lessonId) {
        super("Lesson not found: " + lessonId);
    }
}
