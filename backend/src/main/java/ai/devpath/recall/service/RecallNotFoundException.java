package ai.devpath.recall.service;

public class RecallNotFoundException extends RuntimeException {
    public RecallNotFoundException(String competencyTag) {
        super("No active recall competency found: " + competencyTag);
    }
}
