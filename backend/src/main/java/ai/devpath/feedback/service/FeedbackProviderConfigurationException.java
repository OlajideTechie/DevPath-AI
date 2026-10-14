package ai.devpath.feedback.service;

public class FeedbackProviderConfigurationException extends RuntimeException {
    public FeedbackProviderConfigurationException() {
        super("AI feedback provider is not configured");
    }
}
