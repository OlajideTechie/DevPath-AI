package ai.devpath.recall.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.devpath.feedback.dto.SubmissionResponse;
import ai.devpath.recall.dto.ConfirmDemonstrationRequest;
import ai.devpath.recall.dto.RecallResponse;
import ai.devpath.recall.dto.RecallStatusResponse;
import ai.devpath.recall.dto.RecallSubmissionRequest;
import ai.devpath.recall.service.RecallService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/learners/{learnerId}/recall")
@Tag(name = "Learning Recall", description = "Track competency gaps and related practice")
public class RecallController {
    private final RecallService recallService;

    public RecallController(RecallService recallService) {
        this.recallService = recallService;
    }

    @GetMapping
    @Operation(summary = "Get learner competency recall state and curated transfer exercise")
    public RecallResponse getRecall(@PathVariable UUID learnerId) {
        return recallService.getRecall(learnerId);
    }

    @PostMapping("/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Submit the transfer exercise for advisory feedback",
            description = "Persists the recall attempt; the code is not compiled or executed.",
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Recall attempt and feedback persisted",
                        content = @Content(schema = @Schema(implementation = SubmissionResponse.class))),
                @ApiResponse(responseCode = "400", description = "Invalid submission or competency tag"),
                @ApiResponse(responseCode = "404", description = "Learner or active recall not found"),
                @ApiResponse(responseCode = "503", description = "AI feedback unavailable")
            })
    public SubmissionResponse submitRecall(
            @PathVariable UUID learnerId,
            @Valid @RequestBody RecallSubmissionRequest request) {
        return recallService.submitRecall(learnerId, request.competencyTag(), request.code());
    }

    @PostMapping("/{competencyTag}/demonstration")
    @Operation(
            summary = "Confirm demonstrated competency",
            description = "Requires positive advisory evidence on the original and transfer exercises.")
    public RecallStatusResponse confirmDemonstration(
            @PathVariable UUID learnerId,
            @PathVariable String competencyTag,
            @Valid @RequestBody ConfirmDemonstrationRequest request) {
        return recallService.confirmDemonstration(learnerId, competencyTag, request);
    }
}
