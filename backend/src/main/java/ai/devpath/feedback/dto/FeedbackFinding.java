package ai.devpath.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FeedbackFinding(
        @NotNull Category category,
        @NotNull Competency competency,
        @NotBlank @Size(max = 500) String description) {
    public enum Category {
        FUNCTIONAL_DEFECT,
        OPTIONAL_IMPROVEMENT,
        ADDITIONAL_DEFENSIVE_CHECK,
        PLANNED_REINFORCEMENT
    }

    public enum Competency {
        JAVA_METHODS,
        INPUT_VALIDATION,
        BOUNDARY_CONDITIONS,
        DECIMAL_MONEY,
        NULL_HANDLING
    }
}
