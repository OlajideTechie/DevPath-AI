package ai.devpath.feedback.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ai.devpath.feedback.dto.FeedbackSource;

class FeedbackProviderModeTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ProviderConfiguration.class);

    @Test
    void demoModeSelectsSampleProviderWithoutRequiringLiveProviderConfiguration() {
        contextRunner.withPropertyValues("app.ai.demo-mode=true").run(context -> {
            assertThat(context).hasSingleBean(FeedbackProvider.class);
            FeedbackProvider provider = context.getBean(FeedbackProvider.class);
            assertThat(provider).isInstanceOf(DemoFeedbackProvider.class);
            assertThat(provider.review(null, "not analyzed").feedbackSource())
                    .isEqualTo(FeedbackSource.DEMO_SAMPLE);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({DemoFeedbackProvider.class, OpenAiCompatibleFeedbackProvider.class})
    static class ProviderConfiguration {}
}
