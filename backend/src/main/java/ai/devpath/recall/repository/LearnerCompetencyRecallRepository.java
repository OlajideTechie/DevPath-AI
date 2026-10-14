package ai.devpath.recall.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.devpath.recall.model.LearnerCompetencyRecall;
import ai.devpath.recall.model.LearnerCompetencyRecallId;

public interface LearnerCompetencyRecallRepository
        extends JpaRepository<LearnerCompetencyRecall, LearnerCompetencyRecallId> {
    List<LearnerCompetencyRecall> findAllByIdLearnerIdOrderByIdCompetencyTag(UUID learnerId);

    Optional<LearnerCompetencyRecall> findByIdLearnerIdAndIdCompetencyTag(UUID learnerId, String competencyTag);
}
