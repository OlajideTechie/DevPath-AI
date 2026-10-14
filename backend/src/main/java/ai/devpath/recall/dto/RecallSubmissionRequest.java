package ai.devpath.recall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RecallSubmissionRequest(
        @NotBlank
        @Pattern(regexp = "JAVA_METHODS|INPUT_VALIDATION|BOUNDARY_CONDITIONS|DECIMAL_MONEY|NULL_HANDLING")
        String competencyTag,
        @NotBlank
        @Size(max = 20000)
        String code) {
}
