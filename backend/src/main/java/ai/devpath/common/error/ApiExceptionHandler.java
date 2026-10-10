package ai.devpath.common.error;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import ai.devpath.learner.service.LearnerNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new TreeMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(ApiErrorResponse.validationError(fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.validationError(Map.of("request", "Malformed JSON request")));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String field = exception.getName();
        String message = "learnerId".equals(field) ? "must be a valid UUID" : "has an invalid value";
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.validationError(Map.of(field, message)));
    }

    @ExceptionHandler(LearnerNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleLearnerNotFound(LearnerNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiErrorResponse.learnerNotFound());
    }
}
