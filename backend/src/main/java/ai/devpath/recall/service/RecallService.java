package ai.devpath.recall.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.devpath.curriculum.catalog.CuratedLesson;
import ai.devpath.curriculum.catalog.CuratedLessonCatalog;
import ai.devpath.feedback.dto.AdvisoryFeedback;
import ai.devpath.feedback.dto.AssessmentConfidence;
import ai.devpath.feedback.dto.FeedbackFinding;
import ai.devpath.feedback.dto.SubmissionResponse;
import ai.devpath.feedback.model.LearningAttempt;
import ai.devpath.feedback.repository.LearningAttemptRepository;
import ai.devpath.feedback.service.FeedbackProvider;
import ai.devpath.feedback.service.FeedbackProviderException;
import ai.devpath.learner.model.Learner;
import ai.devpath.learner.repository.LearnerRepository;
import ai.devpath.learner.service.LearnerNotFoundException;
import ai.devpath.recall.dto.CompetencyRecallResponse;
import ai.devpath.recall.dto.ConfirmDemonstrationRequest;
import ai.devpath.recall.dto.RecallExerciseResponse;
import ai.devpath.recall.dto.RecallAttemptResponse;
import ai.devpath.recall.dto.RecallResponse;
import ai.devpath.recall.dto.RecallStatusResponse;
import ai.devpath.recall.model.CompetencyTag;
import ai.devpath.recall.model.LearnerCompetencyRecall;
import ai.devpath.recall.model.RecallStatus;
import ai.devpath.recall.model.RecallReason;
import ai.devpath.recall.repository.LearnerCompetencyRecallRepository;

@Service
public class RecallService {
    private static final String ASSESSMENT_POLICY = """
            A competency is marked DEMONSTRATED only after advisory feedback recommends READY_FOR_REVIEW \
            on both the original exercise and the transfer exercise, identifies the competency in each, \
            and the learner confirms they can explain and apply the rule. Feedback is not code execution.""";

    private final LearnerRepository learnerRepository;
    private final LearningAttemptRepository attemptRepository;
    private final LearnerCompetencyRecallRepository recallRepository;
    private final FeedbackProvider feedbackProvider;
    private final ObjectMapper objectMapper;

    public RecallService(
            LearnerRepository learnerRepository,
            LearningAttemptRepository attemptRepository,
            LearnerCompetencyRecallRepository recallRepository,
            FeedbackProvider feedbackProvider,
            ObjectMapper objectMapper) {
        this.learnerRepository = learnerRepository;
        this.attemptRepository = attemptRepository;
        this.recallRepository = recallRepository;
        this.feedbackProvider = feedbackProvider;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public RecallResponse getRecall(UUID learnerId) {
        learnerRepository.findById(learnerId).orElseThrow(() -> new LearnerNotFoundException(learnerId));
        List<LearnerCompetencyRecall> saved = recallRepository
                .findAllByIdLearnerIdOrderByIdCompetencyTag(learnerId);
        List<CompetencyRecallResponse> competencies = java.util.Arrays.stream(CompetencyTag.values())
                .map(tag -> saved.stream()
                        .filter(record -> record.getCompetencyTag().equals(tag.name()))
                        .findFirst()
                        .map(this::toResponse)
                        .orElseGet(() -> new CompetencyRecallResponse(
                                tag.name(), RecallStatus.NOT_STARTED, null, null, null, false, null, null, null)))
                .toList();
        CuratedLesson lesson = CuratedLessonCatalog.recallLesson();
        RecallExerciseResponse exercise = new RecallExerciseResponse(
                lesson.lessonId(),
                lesson.title(),
                lesson.summary(),
                lesson.learningObjective(),
                lesson.estimatedMinutes(),
                lesson.qaContext(),
                lesson.exercise().prompt(),
                lesson.exercise().requirements(),
                lesson.exercise().testCases(),
                lesson.exercise().starterCode());
        return new RecallResponse(learnerId, exercise, competencies, ASSESSMENT_POLICY);
    }

    @Transactional
    public void recordInitialReview(UUID learnerId, UUID attemptId, AdvisoryFeedback feedback) {
        Learner learner = learnerRepository.findById(learnerId)
                .orElseThrow(() -> new LearnerNotFoundException(learnerId));
        LearningAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalStateException("Saved submission attempt was not found"));
        if (feedback.assessmentConfidence() == AssessmentConfidence.LOW
                || feedback.reviewRecommendation() == AdvisoryFeedback.ReviewRecommendation.UNCERTAIN) {
            return;
        }

        Set<String> defectTags = feedback.findings().stream()
                .filter(finding -> finding.category() == FeedbackFinding.Category.FUNCTIONAL_DEFECT)
                .map(finding -> finding.competency().name())
                .collect(Collectors.toSet());
        Set<String> reinforcementTags = feedback.findings().stream()
                .filter(finding -> finding.category() == FeedbackFinding.Category.PLANNED_REINFORCEMENT)
                .map(finding -> finding.competency().name())
                .filter(tag -> !defectTags.contains(tag))
                .collect(Collectors.toSet());

        for (String tag : defectTags) {
            var recall = recallRepository.findByIdLearnerIdAndIdCompetencyTag(learnerId, tag)
                    .orElseGet(() -> new LearnerCompetencyRecall(learner, tag, attempt));
            recall.identifyGap(attempt);
            recallRepository.save(recall);
        }
        for (String tag : reinforcementTags) {
            var existing = recallRepository.findByIdLearnerIdAndIdCompetencyTag(learnerId, tag);
            if (existing.isEmpty()) {
                LearnerCompetencyRecall recall = new LearnerCompetencyRecall(learner, tag, attempt);
                recall.planReinforcement(attempt);
                recallRepository.save(recall);
            } else if (existing.get().getStatus() != RecallStatus.DEMONSTRATED
                    && existing.get().getReason() == RecallReason.PLANNED_REINFORCEMENT) {
                existing.get().planReinforcement(attempt);
                recallRepository.save(existing.get());
            }
        }

        if (feedback.reviewRecommendation() == AdvisoryFeedback.ReviewRecommendation.READY_FOR_REVIEW) {
            for (String tag : feedback.competencyTags()) {
                if (defectTags.contains(tag) || reinforcementTags.contains(tag)) {
                    continue;
                }
                recallRepository.findByIdLearnerIdAndIdCompetencyTag(learnerId, tag)
                        .filter(recall -> recall.getStatus() != RecallStatus.DEMONSTRATED)
                        .ifPresent(recall -> {
                            recall.recordBaseReady(attempt);
                            recallRepository.save(recall);
                        });
            }
        }
    }

    public SubmissionResponse submitRecall(UUID learnerId, String competencyTag, String code) {
        CompetencyTag tag = parseTag(competencyTag);
        Learner learner = learnerRepository.findById(learnerId)
                .orElseThrow(() -> new LearnerNotFoundException(learnerId));
        LearnerCompetencyRecall recall = recallRepository
                .findByIdLearnerIdAndIdCompetencyTag(learnerId, tag.name())
                .filter(record -> record.getStatus() != RecallStatus.DEMONSTRATED)
                .orElseThrow(() -> new RecallNotFoundException(tag.name()));

        CuratedLesson lesson = CuratedLessonCatalog.recallLesson();
        LearningAttempt attempt = attemptRepository.saveAndFlush(
                new LearningAttempt(learner, lesson.lessonId(), code));
        recall.startRecallPractice();
        recallRepository.saveAndFlush(recall);
        try {
            AdvisoryFeedback feedback = feedbackProvider.review(lesson, code);
            attempt.markFeedbackReady(objectMapper.valueToTree(feedback));
            LearningAttempt savedAttempt = attemptRepository.saveAndFlush(attempt);
            boolean selectedCompetencyHasDefect = feedback.findings().stream()
                    .anyMatch(finding -> finding.category() == FeedbackFinding.Category.FUNCTIONAL_DEFECT
                            && finding.competency().name().equals(tag.name()));
            if (feedback.assessmentConfidence() != AssessmentConfidence.LOW
                    && feedback.reviewRecommendation() == AdvisoryFeedback.ReviewRecommendation.READY_FOR_REVIEW
                    && feedback.competencyTags().contains(tag.name())
                    && !selectedCompetencyHasDefect) {
                recall.recordRecallReady(savedAttempt);
            } else if (selectedCompetencyHasDefect) {
                recall.recordRecallNeedsReview();
            }
            recallRepository.saveAndFlush(recall);
            return new SubmissionResponse(
                    savedAttempt.getId(),
                    learner.getId(),
                    lesson.lessonId(),
                    feedback,
                    true,
                    false,
                    savedAttempt.getCreatedAt());
        } catch (FeedbackProviderException exception) {
            attemptRepository.saveAndFlush(attempt);
            throw exception;
        }
    }

    @Transactional
    public RecallStatusResponse confirmDemonstration(
            UUID learnerId,
            String competencyTag,
            ConfirmDemonstrationRequest request) {
        CompetencyTag tag = parseTag(competencyTag);
        learnerRepository.findById(learnerId).orElseThrow(() -> new LearnerNotFoundException(learnerId));
        LearnerCompetencyRecall recall = recallRepository
                .findByIdLearnerIdAndIdCompetencyTag(learnerId, tag.name())
                .orElseThrow(() -> new RecallNotFoundException(tag.name()));
        if (recall.getStatus() == RecallStatus.DEMONSTRATED) {
            return toStatusResponse(recall);
        }
        if (!request.confirmed()
                || recall.getBaseReadyAttempt() == null
                || recall.getRecallReadyAttempt() == null) {
            throw new RecallEvidenceRequiredException();
        }
        recall.confirmDemonstration();
        return toStatusResponse(recallRepository.saveAndFlush(recall));
    }

    private CompetencyRecallResponse toResponse(LearnerCompetencyRecall recall) {
        return new CompetencyRecallResponse(
                recall.getCompetencyTag(),
                recall.getStatus(),
                recall.getGapAttempt().getId(),
                recall.getBaseReadyAttempt() == null ? null : recall.getBaseReadyAttempt().getId(),
                recall.getRecallReadyAttempt() == null ? null : recall.getRecallReadyAttempt().getId(),
                recall.isLearnerConfirmed(),
                recall.getUpdatedAt(),
                recall.getReason(),
                toSourceAttemptResponse(recall.getGapAttempt()));
    }

    private RecallAttemptResponse toSourceAttemptResponse(LearningAttempt attempt) {
        if (attempt.getFeedback() == null) {
            throw new IllegalStateException("Recall source attempt has no persisted feedback");
        }
        try {
            AdvisoryFeedback feedback = objectMapper
                    .treeToValue(attempt.getFeedback(), AdvisoryFeedback.class)
                    .withCompatibilityDefaults();
            return new RecallAttemptResponse(
                    attempt.getId(),
                    attempt.getLessonId(),
                    attempt.getSubmittedCode(),
                    feedback,
                    attempt.getCreatedAt());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Recall source attempt feedback could not be read", exception);
        }
    }

    private RecallStatusResponse toStatusResponse(LearnerCompetencyRecall recall) {
        return new RecallStatusResponse(
                recall.getCompetencyTag(),
                recall.getStatus(),
                recall.getBaseReadyAttempt() == null ? null : recall.getBaseReadyAttempt().getId(),
                recall.getRecallReadyAttempt() == null ? null : recall.getRecallReadyAttempt().getId(),
                recall.isLearnerConfirmed());
    }

    private CompetencyTag parseTag(String competencyTag) {
        try {
            return CompetencyTag.valueOf(competencyTag);
        } catch (IllegalArgumentException exception) {
            throw new UnsupportedCompetencyTagException();
        }
    }

}
