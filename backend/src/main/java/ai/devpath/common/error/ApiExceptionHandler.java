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
import ai.devpath.curriculum.service.LessonNotFoundException;
import ai.devpath.feedback.service.FeedbackProviderConfigurationException;
import ai.devpath.feedback.service.FeedbackProviderException;
import ai.devpath.recall.service.RecallEvidenceRequiredException;
import ai.devpath.recall.service.RecallNotFoundException;
import ai.devpath.recall.service.UnsupportedCompetencyTagException;

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

    @ExceptionHandler(RecallNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleRecallNotFound(RecallNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse("RECALL_NOT_FOUND", "No active recall found for this competency", null));
    }

    @ExceptionHandler(RecallEvidenceRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleRecallEvidenceRequired(RecallEvidenceRequiredException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse("RECALL_EVIDENCE_REQUIRED", exception.getMessage(), null));
    }

    @ExceptionHandler(UnsupportedCompetencyTagException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedCompetencyTag(
            UnsupportedCompetencyTagException exception) {
        return ResponseEntity.badRequest().body(
                ApiErrorResponse.validationError(Map.of("competencyTag", exception.getMessage())));
    }

    @ExceptionHandler(LessonNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleLessonNotFound(LessonNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse("LESSON_NOT_FOUND", "Lesson not found", null));
    }

    @ExceptionHandler({FeedbackProviderException.class, FeedbackProviderConfigurationException.class})
    public ResponseEntity<ApiErrorResponse> handleFeedbackUnavailable(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiErrorResponse(
                        "AI_FEEDBACK_UNAVAILABLE",
                        "Advisory feedback is temporarily unavailable. Your attempt was recorded; please retry.",
                        null));
    }
}
