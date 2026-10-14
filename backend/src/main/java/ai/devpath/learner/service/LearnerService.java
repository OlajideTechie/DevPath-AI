package ai.devpath.learner.service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.devpath.learner.dto.CreateLearnerRequest;
import ai.devpath.learner.dto.LearnerCreatedResponse;
import ai.devpath.learner.dto.LearnerProfileResponse;
import ai.devpath.learner.model.Learner;
import ai.devpath.learner.model.PreferredLanguage;
import ai.devpath.learner.model.ProfileStatus;
import ai.devpath.learner.repository.LearnerRepository;

@Service
public class LearnerService {
    private static final String CURRENT_ROLE = "QA Engineer";
    private static final String TARGET_ROLE = "Java Backend Developer";

    private final LearnerRepository learnerRepository;

    public LearnerService(LearnerRepository learnerRepository) {
        this.learnerRepository = learnerRepository;
    }

    @Transactional
    public LearnerCreatedResponse createLearner(CreateLearnerRequest request) {
        Set<String> seenSkills = new HashSet<>();
        List<String> existingSkills = request.existingSkills().stream()
                .map(String::trim)
                .filter(skill -> !skill.isEmpty())
                .filter(skill -> seenSkills.add(skill.toLowerCase(Locale.ROOT)))
                .toList();
        List<String> specializations = request.specializations().stream()
                .map(String::trim)
                .toList();
        Learner learner = new Learner(
                CURRENT_ROLE,
                request.experienceBand(),
                specializations,
                existingSkills,
                TARGET_ROLE,
                PreferredLanguage.JAVA,
                request.programmingLevel(),
                request.studyDays(),
                request.dailyMinutes(),
                request.learningStyle(),
                ProfileStatus.CREATED);

        Learner savedLearner = learnerRepository.save(learner);
        return new LearnerCreatedResponse(
                savedLearner.getId(),
                savedLearner.getCurrentRole(),
                savedLearner.getTargetRole(),
                savedLearner.getPreferredLanguage(),
                savedLearner.getProgrammingLevel(),
                savedLearner.getProfileStatus(),
                savedLearner.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public LearnerProfileResponse getLearner(UUID learnerId) {
        Learner learner = learnerRepository.findById(learnerId)
                .orElseThrow(() -> new LearnerNotFoundException(learnerId));
        return new LearnerProfileResponse(
                learner.getId(),
                learner.getCurrentRole(),
                learner.getExperienceBand(),
                learner.getSpecializations(),
                learner.getExistingSkills(),
                learner.getTargetRole(),
                learner.getPreferredLanguage(),
                learner.getProgrammingLevel(),
                learner.getStudyDays(),
                learner.getDailyMinutes(),
                learner.getLearningStyle(),
                learner.getProfileStatus(),
                learner.getCreatedAt());
    }
}
