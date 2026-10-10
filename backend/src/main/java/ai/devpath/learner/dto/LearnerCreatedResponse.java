package ai.devpath.learner.dto;

import java.time.Instant;
import java.util.UUID;

import ai.devpath.learner.model.PreferredLanguage;
import ai.devpath.learner.model.ProfileStatus;
import ai.devpath.learner.model.ProgrammingLevel;

// This record represents the response returned when a learner profile is created.
public record LearnerCreatedResponse(
        UUID id,
        String currentRole,
        String targetRole,
        PreferredLanguage preferredLanguage,
        ProgrammingLevel programmingLevel,
        ProfileStatus profileStatus,
        Instant createdAt) {
}
