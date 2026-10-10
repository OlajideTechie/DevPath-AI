package ai.devpath.learner.service;

import java.util.UUID;

public class LearnerNotFoundException extends RuntimeException {
    public LearnerNotFoundException(UUID learnerId) {
        super("Learner not found: " + learnerId);
    }
}
