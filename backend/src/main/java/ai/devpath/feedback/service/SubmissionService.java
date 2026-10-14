package ai.devpath.feedback.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.devpath.curriculum.catalog.CuratedLesson;
import ai.devpath.curriculum.catalog.CuratedLessonCatalog;
import ai.devpath.curriculum.service.LessonNotFoundException;
import ai.devpath.feedback.dto.AdvisoryFeedback;
import ai.devpath.feedback.dto.SubmissionResponse;
import ai.devpath.feedback.model.LearningAttempt;
import ai.devpath.feedback.repository.LearningAttemptRepository;
import ai.devpath.learner.model.Learner;
import ai.devpath.learner.repository.LearnerRepository;
import ai.devpath.learner.service.LearnerNotFoundException;
import ai.devpath.recall.service.RecallService;

@Service
public class SubmissionService {
    private final LearnerRepository learnerRepository;
    private final LearningAttemptRepository attemptRepository;
    private final FeedbackProvider feedbackProvider;
    private final ObjectMapper objectMapper;
    private final RecallService recallService;

    public SubmissionService(
            LearnerRepository learnerRepository,
            LearningAttemptRepository attemptRepository,
            FeedbackProvider feedbackProvider,
            ObjectMapper objectMapper,
            RecallService recallService) {
        this.learnerRepository = learnerRepository;
        this.attemptRepository = attemptRepository;
        this.feedbackProvider = feedbackProvider;
        this.objectMapper = objectMapper;
        this.recallService = recallService;
    }

    public SubmissionResponse submit(UUID learnerId, String lessonId, String code) {
        Learner learner = learnerRepository.findById(learnerId)
                .orElseThrow(() -> new LearnerNotFoundException(learnerId));
        CuratedLesson lesson = CuratedLessonCatalog.startingLesson();
        if (!lesson.lessonId().equals(lessonId)) {
            throw new LessonNotFoundException(lessonId);
        }

        LearningAttempt attempt = attemptRepository.saveAndFlush(
                new LearningAttempt(learner, lesson.lessonId(), code));
        try {
            AdvisoryFeedback feedback = feedbackProvider.review(lesson, code);
            attempt.markFeedbackReady(objectMapper.valueToTree(feedback));
            LearningAttempt savedAttempt = attemptRepository.saveAndFlush(attempt);
            recallService.recordInitialReview(learner.getId(), savedAttempt.getId(), feedback);
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
}
