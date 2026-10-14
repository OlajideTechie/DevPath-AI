package ai.devpath.recall.service;

public class RecallEvidenceRequiredException extends RuntimeException {
    public RecallEvidenceRequiredException() {
        super("A positive review on both exercises and learner confirmation are required");
    }
}
