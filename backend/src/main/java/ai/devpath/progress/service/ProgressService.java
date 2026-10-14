package ai.devpath.progress.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.devpath.curriculum.catalog.CuratedLesson;
import ai.devpath.curriculum.catalog.CuratedLessonCatalog;
import ai.devpath.feedback.dto.AdvisoryFeedback;
import ai.devpath.feedback.model.AttemptStatus;
import ai.devpath.feedback.model.LearningAttempt;
import ai.devpath.feedback.repository.LearningAttemptRepository;
import ai.devpath.learner.repository.LearnerRepository;
import ai.devpath.learner.service.LearnerNotFoundException;
import ai.devpath.progress.dto.LatestFeedbackResponse;
import ai.devpath.progress.dto.NextActivityResponse;
import ai.devpath.progress.dto.ProgressActivityType;
import ai.devpath.progress.dto.ProgressCompetencyResponse;
import ai.devpath.progress.dto.ProgressResponse;
import ai.devpath.recall.model.LearnerCompetencyRecall;
import ai.devpath.recall.model.RecallStatus;
import ai.devpath.recall.repository.LearnerCompetencyRecallRepository;

@Service
public class ProgressService {
    private final LearnerRepository learnerRepository;
    private final LearningAttemptRepository attemptRepository;
    private final LearnerCompetencyRecallRepository recallRepository;
    private final ObjectMapper objectMapper;

    public ProgressService(
            LearnerRepository learnerRepository,
            LearningAttemptRepository attemptRepository,
            LearnerCompetencyRecallRepository recallRepository,
            ObjectMapper objectMapper) {
        this.learnerRepository = learnerRepository;
        this.attemptRepository = attemptRepository;
        this.recallRepository = recallRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ProgressResponse getProgress(UUID learnerId) {
        learnerRepository.findById(learnerId)
                .orElseThrow(() -> new LearnerNotFoundException(learnerId));

        List<LearnerCompetencyRecall> recalls = recallRepository
                .findAllByIdLearnerIdOrderByIdCompetencyTag(learnerId);
        List<ProgressCompetencyResponse> needingReview = recalls.stream()
                .filter(recall -> recall.getStatus() == RecallStatus.NEEDS_REVIEW)
                .map(this::toCompetencyResponse)
                .toList();
        List<ProgressCompetencyResponse> inProgress = recalls.stream()
                .filter(recall -> recall.getStatus() == RecallStatus.PRACTICING)
                .map(this::toCompetencyResponse)
                .toList();
        List<ProgressCompetencyResponse> demonstrated = recalls.stream()
                .filter(recall -> recall.getStatus() == RecallStatus.DEMONSTRATED)
                .map(this::toCompetencyResponse)
                .toList();
        long lessonsAttempted = attemptRepository.countDistinctLessonsByLearnerId(learnerId);
        long exercisesSubmitted = attemptRepository.countByLearner_Id(learnerId);

        return new ProgressResponse(
                learnerId,
                lessonsAttempted,
                exercisesSubmitted,
                needingReview,
                inProgress,
                demonstrated,
                attemptRepository.findFirstByLearner_IdAndStatusOrderByCreatedAtDesc(
                                learnerId, AttemptStatus.FEEDBACK_READY)
                        .map(this::toLatestFeedbackResponse)
                        .orElse(null),
                nextActivity(needingReview, inProgress, recalls, exercisesSubmitted));
    }

    private ProgressCompetencyResponse toCompetencyResponse(LearnerCompetencyRecall recall) {
        return new ProgressCompetencyResponse(
                recall.getCompetencyTag(),
                recall.getStatus(),
                recall.getUpdatedAt());
    }

    private LatestFeedbackResponse toLatestFeedbackResponse(LearningAttempt attempt) {
        if (attempt.getFeedback() == null) {
            throw new IllegalStateException(
                    "A feedback-ready learning attempt has no persisted feedback");
        }
        try {
            AdvisoryFeedback feedback = objectMapper.treeToValue(
                    attempt.getFeedback(), AdvisoryFeedback.class).withCompatibilityDefaults();
            return new LatestFeedbackResponse(
                    attempt.getId(),
                    attempt.getLessonId(),
                    attempt.getCreatedAt(),
                    feedback.summary(),
                    feedback.nextAction(),
                    feedback.competencyTags(),
                    feedback.reviewRecommendation(),
                    feedback.feedbackSource());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Persisted advisory feedback could not be read", exception);
        }
    }

    private NextActivityResponse nextActivity(
            List<ProgressCompetencyResponse> needingReview,
            List<ProgressCompetencyResponse> inProgress,
            List<LearnerCompetencyRecall> recalls,
            long exercisesSubmitted) {
        CuratedLesson recallLesson = CuratedLessonCatalog.recallLesson();
        CuratedLesson starterLesson = CuratedLessonCatalog.startingLesson();

        LearnerCompetencyRecall readyToConfirm = recalls.stream()
                .filter(recall -> recall.getStatus() == RecallStatus.PRACTICING)
                .filter(recall -> recall.getBaseReadyAttempt() != null
                        && recall.getRecallReadyAttempt() != null)
                .findFirst()
                .orElse(null);
        if (readyToConfirm != null) {
            return recallActivity(
                    recallLesson,
                    readyToConfirm.getCompetencyTag(),
                    "Review the positive advisory evidence and confirm whether you can explain and apply "
                            + readyToConfirm.getCompetencyTag() + ".");
        }

        if (!needingReview.isEmpty()) {
            ProgressCompetencyResponse competency = needingReview.getFirst();
            return recallActivity(
                    recallLesson,
                    competency.competencyTag(),
                    "Practise the transfer exercise for " + competency.competencyTag()
                            + ", which is currently marked NEEDS_REVIEW.");
        }

        if (!inProgress.isEmpty()) {
            ProgressCompetencyResponse competency = inProgress.getFirst();
            LearnerCompetencyRecall record = recalls.stream()
                    .filter(recall -> recall.getCompetencyTag().equals(competency.competencyTag()))
                    .findFirst()
                    .orElseThrow();
            if (record.getBaseReadyAttempt() == null && record.getRecallReadyAttempt() != null) {
                return lessonActivity(
                        starterLesson,
                        "Get a positive review on the original exercise for "
                                + competency.competencyTag() + " to complete its evidence pair.");
            }
            return recallActivity(
                    recallLesson,
                    competency.competencyTag(),
                    "Continue transfer practice for " + competency.competencyTag()
                            + " before treating it as demonstrated.");
        }

        return lessonActivity(
                starterLesson,
                exercisesSubmitted == 0
                        ? "Start the curated Java methods and input-validation lesson. "
                                + "No exercise submissions are recorded yet."
                        : "Continue the curated Java methods and input-validation practice. "
                                + "No competency currently requires a recall follow-up.");
    }

    private NextActivityResponse recallActivity(
            CuratedLesson lesson,
            String competencyTag,
            String reason) {
        return new NextActivityResponse(
                ProgressActivityType.RECALL,
                lesson.lessonId(),
                lesson.title(),
                lesson.estimatedMinutes(),
                reason,
                competencyTag);
    }

    private NextActivityResponse lessonActivity(CuratedLesson lesson, String reason) {
        return new NextActivityResponse(
                ProgressActivityType.LESSON,
                lesson.lessonId(),
                lesson.title(),
                lesson.estimatedMinutes(),
                reason,
                null);
    }
}
