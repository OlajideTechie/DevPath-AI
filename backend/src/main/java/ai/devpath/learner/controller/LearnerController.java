package ai.devpath.learner.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.devpath.learner.dto.CreateLearnerRequest;
import ai.devpath.learner.dto.LearnerCreatedResponse;
import ai.devpath.learner.dto.LearnerProfileResponse;
import ai.devpath.learner.service.LearnerService;
import ai.devpath.common.error.ApiErrorResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/learners")
@Tag(name = "Learners", description = "Create and retrieve learner profiles")
public class LearnerController {
    private final LearnerService learnerService;

    public LearnerController(LearnerService learnerService) {
        this.learnerService = learnerService;
    }

    @PostMapping
    @Operation(
            summary = "Create a learner profile",
            description = "Creates a learner profile using server-controlled target role, language, and status.",
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Learner profile created",
                        headers = @Header(
                                name = "Location",
                                description = "URI of the created learner profile",
                                schema = @Schema(type = "string")),
                        content = @Content(schema = @Schema(implementation = LearnerCreatedResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid learner profile",
                        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public ResponseEntity<LearnerCreatedResponse> createLearner(
            @Valid @RequestBody CreateLearnerRequest request) {
        LearnerCreatedResponse response = learnerService.createLearner(request);
        URI location = URI.create("/api/v1/learners/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{learnerId}")
    @Operation(
            summary = "Get a learner profile",
            description = "Returns the persisted learner profile for the supplied UUID.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Learner profile found",
                        content = @Content(schema = @Schema(implementation = LearnerProfileResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Malformed learner UUID",
                        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "Learner not found",
                        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public LearnerProfileResponse getLearner(
            @Parameter(description = "Learner profile UUID", required = true) @PathVariable UUID learnerId) {
        return learnerService.getLearner(learnerId);
    }
}
