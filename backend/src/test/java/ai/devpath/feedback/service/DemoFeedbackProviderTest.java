package ai.devpath.feedback.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ai.devpath.curriculum.catalog.CuratedLessonCatalog;
import ai.devpath.feedback.dto.AdvisoryFeedback;
import ai.devpath.feedback.dto.FeedbackSource;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

class DemoFeedbackProviderTest {
    private final DemoFeedbackProvider provider = new DemoFeedbackProvider();

    @Test
    void returnsClearlyLabeledUncertainSampleWithoutAssessingOrCreatingFindings() {
        AdvisoryFeedback feedback = provider.review(
                CuratedLessonCatalog.startingLesson(),
                "not analyzed by the demo provider");

        assertThat(feedback.feedbackSource()).isEqualTo(FeedbackSource.DEMO_SAMPLE);
        assertThat(feedback.summary()).contains("DEMO SAMPLE ONLY", "does not assess");
        assertThat(feedback.reviewRecommendation())
                .isEqualTo(AdvisoryFeedback.ReviewRecommendation.UNCERTAIN);
        assertThat(feedback.findings()).isEmpty();
        assertThat(feedback.assessmentConfidence().name()).isEqualTo("LOW");
        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(validatorFactory.getValidator().validate(feedback)).isEmpty();
        }
    }
}
