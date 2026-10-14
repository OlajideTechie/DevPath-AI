package ai.devpath.progress.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.devpath.common.error.ApiErrorResponse;
import ai.devpath.progress.dto.ProgressResponse;
import ai.devpath.progress.service.ProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/learners/{learnerId}/progress")
@Tag(name = "Learning progress", description = "Persisted learner attempts and competency progress")
public class ProgressController {
    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping
    @Operation(
            summary = "Get persisted learner progress",
            description = "Reports persisted attempts, recall statuses, latest feedback, and a deterministic next activity.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Progress summary returned",
                        content = @Content(schema = @Schema(implementation = ProgressResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Malformed learner UUID",
                        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "Learner not found",
                        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public ProgressResponse getProgress(@PathVariable UUID learnerId) {
        return progressService.getProgress(learnerId);
    }
}
