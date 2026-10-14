package ai.devpath.recall.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record ConfirmDemonstrationRequest(
        @NotNull
        @AssertTrue(message = "must confirm the assessment policy")
        Boolean confirmed) {
}
