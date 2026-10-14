package ai.devpath.feedback.service;

import ai.devpath.curriculum.catalog.CuratedLesson;
import ai.devpath.feedback.dto.AdvisoryFeedback;

public interface FeedbackProvider {
    AdvisoryFeedback review(CuratedLesson lesson, String code);
}
