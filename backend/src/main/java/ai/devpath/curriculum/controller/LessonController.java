package ai.devpath.curriculum.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.devpath.common.error.ApiErrorResponse;
import ai.devpath.curriculum.catalog.CuratedLesson;
import ai.devpath.curriculum.catalog.CuratedLessonCatalog;
import ai.devpath.curriculum.dto.LessonContentResponse;
import ai.devpath.curriculum.service.LessonNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/lessons")
@Tag(name = "Curriculum", description = "Curated lesson content and recommendations")
public class LessonController {
    @GetMapping("/{lessonId}")
    @Operation(
            summary = "Get curated lesson content",
            description = "Returns the curated lesson explanation and practice exercise.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Lesson found",
                        content = @Content(schema = @Schema(implementation = LessonContentResponse.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "Lesson not found",
                        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public LessonContentResponse getLesson(@PathVariable String lessonId) {
        CuratedLesson lesson = CuratedLessonCatalog.startingLesson();
        if (!lesson.lessonId().equals(lessonId)) {
            throw new LessonNotFoundException(lessonId);
        }
        return new LessonContentResponse(
                lesson.lessonId(),
                lesson.title(),
                lesson.summary(),
                lesson.learningObjective(),
                lesson.estimatedMinutes(),
                lesson.qaContext(),
                lesson.sections(),
                lesson.exercise());
    }
}
