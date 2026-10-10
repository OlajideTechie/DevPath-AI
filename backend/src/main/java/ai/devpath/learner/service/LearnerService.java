package ai.devpath.learner.service;

import java.util.List;
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
    private static final String TARGET_ROLE = "Java Backend Developer";

    private final LearnerRepository learnerRepository;

    public LearnerService(LearnerRepository learnerRepository) {
        this.learnerRepository = learnerRepository;
    }
    
    // This method creates a new learner profile based on the provided request and 
    // returns a response containing the created learner's details.
    @Transactional
    public LearnerCreatedResponse createLearner(CreateLearnerRequest request) {
        List<String> existingSkills = request.existingSkills().stream()
                .map(String::trim)
                .distinct()
                .toList();
        Learner learner = new Learner(
                request.currentRole(),
                request.yearsOfExperience(),
                request.specialization(),
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
                learner.getYearsOfExperience(),
                learner.getSpecialization(),
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
