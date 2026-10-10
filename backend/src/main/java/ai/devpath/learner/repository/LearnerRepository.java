package ai.devpath.learner.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.devpath.learner.model.Learner;

public interface LearnerRepository extends JpaRepository<Learner, UUID> {
}
