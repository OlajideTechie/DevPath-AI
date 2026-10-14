package ai.devpath.curriculum.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.devpath.common.error.ApiErrorResponse;
import ai.devpath.curriculum.dto.RecommendedLessonResponse;
import ai.devpath.curriculum.service.LessonRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/learners/{learnerId}/recommended-lesson")
@Tag(name = "Curriculum", description = "Deterministic curated lesson recommendations")
public class LessonRecommendationController {
    private final LessonRecommendationService recommendationService;

    public LessonRecommendationController(LessonRecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    @Operation(
            summary = "Get a learner's recommended starting lesson",
            description = "Selects the first curated lesson deterministically and adapts its QA example to the learner profile.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Recommended lesson found",
                        content = @Content(schema = @Schema(implementation = RecommendedLessonResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Malformed learner UUID",
                        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "Learner not found",
                        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public RecommendedLessonResponse getRecommendedLesson(
            @Parameter(description = "Learner profile UUID", required = true) @PathVariable UUID learnerId) {
        return recommendationService.recommendFor(learnerId);
    }
}
