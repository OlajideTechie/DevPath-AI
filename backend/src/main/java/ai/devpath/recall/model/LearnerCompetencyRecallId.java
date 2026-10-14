package ai.devpath.recall.model;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class LearnerCompetencyRecallId implements Serializable {
    @Column(name = "learner_id")
    private UUID learnerId;

    @Column(name = "competency_tag", length = 80)
    private String competencyTag;

    protected LearnerCompetencyRecallId() {
    }

    public LearnerCompetencyRecallId(UUID learnerId, String competencyTag) {
        this.learnerId = learnerId;
        this.competencyTag = competencyTag;
    }

    public UUID getLearnerId() {
        return learnerId;
    }

    public String getCompetencyTag() {
        return competencyTag;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LearnerCompetencyRecallId that)) return false;
        return java.util.Objects.equals(learnerId, that.learnerId)
                && java.util.Objects.equals(competencyTag, that.competencyTag);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(learnerId, competencyTag);
    }
}
