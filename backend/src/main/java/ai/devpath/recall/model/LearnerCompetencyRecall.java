package ai.devpath.recall.model;

import java.time.Instant;

import ai.devpath.feedback.model.LearningAttempt;
import ai.devpath.learner.model.Learner;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "learner_competency_recall")
public class LearnerCompetencyRecall {
    @EmbeddedId
    private LearnerCompetencyRecallId id;

    @MapsId("learnerId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "learner_id", nullable = false)
    private Learner learner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecallStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecallReason reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gap_attempt_id", nullable = false)
    private LearningAttempt gapAttempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_ready_attempt_id")
    private LearningAttempt baseReadyAttempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recall_ready_attempt_id")
    private LearningAttempt recallReadyAttempt;

    @Column(name = "learner_confirmed", nullable = false)
    private boolean learnerConfirmed;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected LearnerCompetencyRecall() {
    }

    public LearnerCompetencyRecall(
            Learner learner,
            String competencyTag,
            LearningAttempt gapAttempt) {
        this.id = new LearnerCompetencyRecallId(learner.getId(), competencyTag);
        this.learner = learner;
        this.gapAttempt = gapAttempt;
        this.reason = RecallReason.COMPETENCY_GAP;
        this.status = RecallStatus.NEEDS_REVIEW;
    }

    public void identifyGap(LearningAttempt attempt) {
        gapAttempt = attempt;
        reason = RecallReason.COMPETENCY_GAP;
        baseReadyAttempt = null;
        recallReadyAttempt = null;
        learnerConfirmed = false;
        status = RecallStatus.NEEDS_REVIEW;
    }

    public void planReinforcement(LearningAttempt attempt) {
        gapAttempt = attempt;
        reason = RecallReason.PLANNED_REINFORCEMENT;
        baseReadyAttempt = attempt;
        recallReadyAttempt = null;
        learnerConfirmed = false;
        status = RecallStatus.PRACTICING;
    }

    public void recordBaseReady(LearningAttempt attempt) {
        baseReadyAttempt = attempt;
        learnerConfirmed = false;
        status = RecallStatus.PRACTICING;
    }

    public void startRecallPractice() {
        recallReadyAttempt = null;
        learnerConfirmed = false;
        status = RecallStatus.PRACTICING;
    }

    public void recordRecallReady(LearningAttempt attempt) {
        recallReadyAttempt = attempt;
        learnerConfirmed = false;
        status = RecallStatus.PRACTICING;
    }

    public void recordRecallNeedsReview() {
        recallReadyAttempt = null;
        baseReadyAttempt = null;
        learnerConfirmed = false;
        status = RecallStatus.NEEDS_REVIEW;
    }

    public void confirmDemonstration() {
        learnerConfirmed = true;
        status = RecallStatus.DEMONSTRATED;
    }

    @PrePersist
    @jakarta.persistence.PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }

    public String getCompetencyTag() {
        return id.getCompetencyTag();
    }

    public RecallStatus getStatus() {
        return status;
    }

    public RecallReason getReason() {
        return reason;
    }

    public LearningAttempt getGapAttempt() {
        return gapAttempt;
    }

    public LearningAttempt getBaseReadyAttempt() {
        return baseReadyAttempt;
    }

    public LearningAttempt getRecallReadyAttempt() {
        return recallReadyAttempt;
    }

    public boolean isLearnerConfirmed() {
        return learnerConfirmed;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
