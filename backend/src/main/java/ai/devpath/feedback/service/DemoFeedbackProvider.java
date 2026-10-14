package ai.devpath.feedback.service;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import ai.devpath.curriculum.catalog.CuratedLesson;
import ai.devpath.feedback.dto.AdvisoryFeedback;
import ai.devpath.feedback.dto.AssessmentConfidence;
import ai.devpath.feedback.dto.FeedbackSource;
import ai.devpath.feedback.dto.NextActionType;

@Component
@ConditionalOnProperty(prefix = "app.ai", name = "demo-mode", havingValue = "true")
public class DemoFeedbackProvider implements FeedbackProvider {
    @Override
    public AdvisoryFeedback review(CuratedLesson lesson, String code) {
        return new AdvisoryFeedback(
                "DEMO SAMPLE ONLY: This response does not assess your submitted code.",
                List.of("Sample only: no submission-specific strengths were assessed."),
                List.of("Your code was not analyzed because demo feedback mode is enabled."),
                List.of("Review the exercise requirements manually; this sample cannot recommend a code change."),
                List.of("INPUT_VALIDATION"),
                "Use the exercise requirements to review your code, or disable demo mode to request live provider feedback.",
                AdvisoryFeedback.ReviewRecommendation.UNCERTAIN,
                List.of(),
                AssessmentConfidence.LOW,
                NextActionType.CLARIFICATION,
                FeedbackSource.DEMO_SAMPLE);
    }
}
