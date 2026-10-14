package ai.devpath.curriculum.service;

import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.devpath.curriculum.catalog.CuratedLesson;
import ai.devpath.curriculum.catalog.CuratedLessonCatalog;
import ai.devpath.curriculum.dto.RecommendedLessonResponse;
import ai.devpath.learner.model.Learner;
import ai.devpath.learner.model.ProgrammingLevel;
import ai.devpath.learner.repository.LearnerRepository;
import ai.devpath.learner.service.LearnerNotFoundException;

@Service
public class LessonRecommendationService {
    private final LearnerRepository learnerRepository;

    public LessonRecommendationService(LearnerRepository learnerRepository) {
        this.learnerRepository = learnerRepository;
    }

    @Transactional(readOnly = true)
    public RecommendedLessonResponse recommendFor(UUID learnerId) {
        Learner learner = learnerRepository.findById(learnerId)
                .orElseThrow(() -> new LearnerNotFoundException(learnerId));
        CuratedLesson lesson = CuratedLessonCatalog.startingLesson();

        return new RecommendedLessonResponse(
                lesson.lessonId(),
                lesson.title(),
                lesson.summary(),
                lesson.learningObjective(),
                lesson.estimatedMinutes(),
                recommendationReason(learner.getProgrammingLevel()),
                qaScenario(learner));
    }

    private String recommendationReason(ProgrammingLevel programmingLevel) {
        return switch (programmingLevel) {
            case BEGINNER -> "Start with one focused Java method: identify its inputs, check for missing or non-positive values, then compare the amount.";
            case INTERMEDIATE -> "Trace how guard clauses validate inputs before business-rule comparisons, and how BigDecimal compares monetary values.";
            case EXPERIENCED -> "Review method boundaries, null safety, and BigDecimal value semantics as a concise example of defensive backend validation.";
        };
    }

    private String qaScenario(Learner learner) {
        String guidance = switch (learner.getExperienceBand()) {
            case ZERO_TO_ONE -> "Start with three straightforward cases: an accepted withdrawal, a zero amount, and an amount above the balance.";
            case TWO_TO_THREE -> "Try valid and invalid requests, including the exact-balance boundary.";
            case FOUR_TO_FIVE -> "Build a focused regression set for null, non-positive, exact-balance, and over-balance requests.";
            case SIX_TO_EIGHT -> "Design boundary-focused coverage across valid, malformed, and over-balance requests, and verify rejected requests do not change the balance.";
            case NINE_PLUS -> "Define a layered contract and regression strategy for input partitions, monetary boundaries, rejection semantics, and account-state invariants.";
        };

        return guidance + " " + specializationFocus(learner);
    }

    private String specializationFocus(Learner learner) {
        if (hasSpecialization(learner, "API Testing")) {
            return "Apply your API testing experience to check the withdrawal endpoint's accepted and rejected responses.";
        }
        if (hasSpecialization(learner, "Database Testing")) {
            return "Apply your database testing experience by checking that rejected requests leave the stored balance unchanged.";
        }
        if (hasSpecialization(learner, "Manual Testing")) {
            return "Apply your manual testing experience by observing the response for each case and comparing it with the expected behavior.";
        }
        if (hasSpecialization(learner, "Test Automation")) {
            return "Apply your test automation experience by turning the cases into repeatable acceptance checks.";
        }
        if (hasSpecialization(learner, "Mobile Application Testing")) {
            return "Apply your mobile application testing experience by checking how the banking app handles accepted and rejected withdrawal requests.";
        }
        if (hasSpecialization(learner, "Performance Testing")) {
            return "Apply your performance testing experience by checking that validation results stay consistent across request sequences.";
        }
        if (hasSpecialization(learner, "Security Testing")) {
            return "Apply your security testing experience by checking that invalid requests cannot bypass the validation rules.";
        }
        if (hasSpecialization(learner, "Quality Engineering / SDET")) {
            return "Apply your quality engineering experience by defining contract checks for accepted and rejected cases.";
        }
        if (hasSpecialization(learner, "Test Leadership / QA Management")) {
            return "Apply your QA leadership experience by framing clear acceptance criteria for each case.";
        }
        return "Use your QA experience to check the expected result for each request.";
    }

    private boolean hasSpecialization(Learner learner, String expected) {
        return learner.getSpecializations().stream()
                .filter(specialization -> specialization != null)
                .map(specialization -> specialization.trim().toLowerCase(Locale.ROOT))
                .anyMatch(specialization -> specialization.equals(expected.toLowerCase(Locale.ROOT)));
    }
}
