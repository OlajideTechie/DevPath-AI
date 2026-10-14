package ai.devpath.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitCodeRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = 20000, message = "must be at most 20000 characters")
        String code) {
}
