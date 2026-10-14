package ai.devpath.feedback.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

public record AdvisoryFeedback(
        @NotBlank @Size(max = 1000) String summary,
        @NotEmpty @Size(max = 8) List<@NotBlank @Size(max = 300) String> strengths,
        @NotNull @Size(max = 8) List<@NotBlank @Size(max = 500) String> issues,
        @NotNull @Size(max = 8) List<@NotBlank @Size(max = 500) String> improvements,
        @NotEmpty @Size(max = 8) List<@NotBlank @Size(max = 80) String> competencyTags,
        @NotBlank @Size(max = 500) String nextAction,
        @NotNull ReviewRecommendation reviewRecommendation,
        @NotNull @Size(max = 8) List<@NotNull @Valid FeedbackFinding> findings,
        @NotNull AssessmentConfidence assessmentConfidence,
        @NotNull NextActionType nextActionType,
        @NotNull FeedbackSource feedbackSource) {
    public AdvisoryFeedback(
            String summary,
            List<String> strengths,
            List<String> issues,
            List<String> improvements,
            List<String> competencyTags,
            String nextAction,
            ReviewRecommendation reviewRecommendation,
            List<FeedbackFinding> findings,
            AssessmentConfidence assessmentConfidence,
            NextActionType nextActionType) {
        this(
                summary,
                strengths,
                issues,
                improvements,
                competencyTags,
                nextAction,
                reviewRecommendation,
                findings,
                assessmentConfidence,
                nextActionType,
                FeedbackSource.LIVE_PROVIDER);
    }

    public AdvisoryFeedback(
            String summary,
            List<String> strengths,
            List<String> issues,
            List<String> improvements,
            List<String> competencyTags,
            String nextAction,
            ReviewRecommendation reviewRecommendation) {
        this(
                summary,
                strengths,
                issues,
                improvements,
                competencyTags,
                nextAction,
                reviewRecommendation,
                List.of(),
                AssessmentConfidence.MEDIUM,
                defaultNextActionType(reviewRecommendation),
                FeedbackSource.LIVE_PROVIDER);
    }

    private static NextActionType defaultNextActionType(ReviewRecommendation recommendation) {
        return switch (recommendation) {
            case NEEDS_REVIEW -> NextActionType.TARGETED_CORRECTION;
            case CONTINUE_PRACTICE -> NextActionType.REINFORCEMENT;
            case READY_FOR_REVIEW -> NextActionType.NEXT_LESSON;
            case UNCERTAIN -> NextActionType.CLARIFICATION;
        };
    }

    public AdvisoryFeedback withCompatibilityDefaults() {
        return new AdvisoryFeedback(
                summary,
                strengths,
                issues,
                improvements,
                competencyTags,
                nextAction,
                reviewRecommendation,
                findings == null ? List.of() : findings,
                assessmentConfidence == null ? AssessmentConfidence.MEDIUM : assessmentConfidence,
                nextActionType == null ? defaultNextActionType(reviewRecommendation) : nextActionType,
                feedbackSource == null ? FeedbackSource.LIVE_PROVIDER : feedbackSource);
    }

    public enum ReviewRecommendation {
        NEEDS_REVIEW,
        CONTINUE_PRACTICE,
        READY_FOR_REVIEW,
        UNCERTAIN
    }
}
