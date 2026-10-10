package ai.devpath.common.error;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(String code, String message, Map<String, String> fieldErrors) {
    public static ApiErrorResponse validationError(Map<String, String> fieldErrors) {
        return new ApiErrorResponse("VALIDATION_ERROR", "Invalid learner profile", fieldErrors);
    }

    public static ApiErrorResponse learnerNotFound() {
        return new ApiErrorResponse("LEARNER_NOT_FOUND", "Learner not found", null);
    }
}
