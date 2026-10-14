package ai.devpath.recall.dto;

import java.util.List;

public record RecallExerciseResponse(
        String lessonId,
        String title,
        String summary,
        String learningObjective,
        int estimatedMinutes,
        String qaContext,
        String prompt,
        List<String> requirements,
        List<String> testCases,
        String starterCode) {
}
