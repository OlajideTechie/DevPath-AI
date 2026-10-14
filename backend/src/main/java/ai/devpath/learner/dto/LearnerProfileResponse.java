package ai.devpath.learner.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ai.devpath.learner.model.ExperienceBand;
import ai.devpath.learner.model.LearningStyle;
import ai.devpath.learner.model.PreferredLanguage;
import ai.devpath.learner.model.ProfileStatus;
import ai.devpath.learner.model.ProgrammingLevel;
import ai.devpath.learner.model.StudyDay;

// This record represents the response returned when a learner profile is retrieved.
public record LearnerProfileResponse(
        UUID id,
        String currentRole,
        ExperienceBand experienceBand,
        List<String> specializations,
        List<String> existingSkills,
        String targetRole,
        PreferredLanguage preferredLanguage,
        ProgrammingLevel programmingLevel,
        List<StudyDay> studyDays,
        int dailyMinutes,
        LearningStyle learningStyle,
        ProfileStatus profileStatus,
        Instant createdAt) {
}
