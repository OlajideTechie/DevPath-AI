package ai.devpath.feedback.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

import ai.devpath.learner.model.Learner;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "learning_attempts")
public class LearningAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "learner_id", nullable = false)
    private Learner learner;

    @Column(name = "lesson_id", nullable = false, length = 100)
    private String lessonId;

    @Column(name = "submitted_code", nullable = false, columnDefinition = "text")
    private String submittedCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AttemptStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "feedback_json", columnDefinition = "jsonb")
    private JsonNode feedback;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LearningAttempt() {
    }

    public LearningAttempt(Learner learner, String lessonId, String submittedCode) {
        this.learner = learner;
        this.lessonId = lessonId;
        this.submittedCode = submittedCode;
        this.status = AttemptStatus.FEEDBACK_FAILED;
    }

    public void markFeedbackReady(JsonNode feedback) {
        this.feedback = feedback;
        this.status = AttemptStatus.FEEDBACK_READY;
    }

    @PrePersist
    void setCreatedAt() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Learner getLearner() {
        return learner;
    }

    public String getLessonId() {
        return lessonId;
    }

    public String getSubmittedCode() {
        return submittedCode;
    }

    public AttemptStatus getStatus() {
        return status;
    }

    public JsonNode getFeedback() {
        return feedback;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
