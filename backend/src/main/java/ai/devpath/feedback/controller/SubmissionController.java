package ai.devpath.feedback.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.devpath.common.error.ApiErrorResponse;
import ai.devpath.feedback.dto.SubmissionResponse;
import ai.devpath.feedback.dto.SubmitCodeRequest;
import ai.devpath.feedback.service.SubmissionService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/learners/{learnerId}/lessons/{lessonId}/submissions")
@Tag(name = "Learning feedback", description = "Submit lesson code for advisory feedback")
public class SubmissionController {
    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Submit lesson code for advisory feedback",
            description = "Persists the attempt and requests advisory feedback. Code is not compiled or executed.",
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Attempt and feedback persisted",
                        content = @Content(schema = @Schema(implementation = SubmissionResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid submission",
                        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "Learner or lesson not found",
                        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                @ApiResponse(
                        responseCode = "503",
                        description = "AI feedback unavailable",
                        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public SubmissionResponse submit(
            @PathVariable UUID learnerId,
            @PathVariable String lessonId,
            @Valid @RequestBody SubmitCodeRequest request) {
        return submissionService.submit(learnerId, lessonId, request.code());
    }
}
