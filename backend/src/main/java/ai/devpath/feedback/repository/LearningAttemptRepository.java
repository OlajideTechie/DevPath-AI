package ai.devpath.feedback.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.devpath.feedback.model.AttemptStatus;
import ai.devpath.feedback.model.LearningAttempt;

public interface LearningAttemptRepository extends JpaRepository<LearningAttempt, UUID> {
    long countByLearner_Id(UUID learnerId);

    @Query("select count(distinct attempt.lessonId) from LearningAttempt attempt "
            + "where attempt.learner.id = :learnerId")
    long countDistinctLessonsByLearnerId(@Param("learnerId") UUID learnerId);

    Optional<LearningAttempt> findFirstByLearner_IdAndStatusOrderByCreatedAtDesc(
            UUID learnerId,
            AttemptStatus status);
}
