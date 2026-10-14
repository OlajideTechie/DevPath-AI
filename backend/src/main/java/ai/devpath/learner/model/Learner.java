package ai.devpath.learner.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "learners")
public class Learner {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "\"current_role\"", nullable = false, length = 100)
    private String currentRole;

    @Column(name = "years_of_experience", nullable = false)
    private int yearsOfExperience;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_band", nullable = false, length = 20)
    private ExperienceBand experienceBand;

    @Column(nullable = false, length = 100)
    private String specialization;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "specializations", nullable = false, columnDefinition = "jsonb")
    private List<String> specializations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "existing_skills", nullable = false, columnDefinition = "jsonb")
    private List<String> existingSkills;

    @Column(name = "target_role", nullable = false, length = 100)
    private String targetRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_language", nullable = false, length = 20)
    private PreferredLanguage preferredLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "programming_level", nullable = false, length = 20)
    private ProgrammingLevel programmingLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "study_days", nullable = false, columnDefinition = "jsonb")
    private List<StudyDay> studyDays;

    @Column(name = "daily_minutes", nullable = false)
    private int dailyMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "learning_style", nullable = false, length = 30)
    private LearningStyle learningStyle;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_status", nullable = false, length = 30)
    private ProfileStatus profileStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Learner() {
    }

    public Learner(
            String currentRole,
            ExperienceBand experienceBand,
            List<String> specializations,
            List<String> existingSkills,
            String targetRole,
            PreferredLanguage preferredLanguage,
            ProgrammingLevel programmingLevel,
            List<StudyDay> studyDays,
            int dailyMinutes,
            LearningStyle learningStyle,
            ProfileStatus profileStatus) {
        this.currentRole = currentRole;
        this.yearsOfExperience = experienceBand.lowerBoundYears();
        this.experienceBand = experienceBand;
        this.specialization = specializations.get(0);
        this.specializations = List.copyOf(specializations);
        this.existingSkills = List.copyOf(existingSkills);
        this.targetRole = targetRole;
        this.preferredLanguage = preferredLanguage;
        this.programmingLevel = programmingLevel;
        this.studyDays = List.copyOf(studyDays);
        this.dailyMinutes = dailyMinutes;
        this.learningStyle = learningStyle;
        this.profileStatus = profileStatus;
    }

    @PrePersist
    void setCreationTimestamps() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getCurrentRole() {
        return currentRole;
    }

    public int getYearsOfExperience() {
        return yearsOfExperience;
    }

    public ExperienceBand getExperienceBand() {
        return experienceBand;
    }

    public String getSpecialization() {
        return specialization;
    }

    public List<String> getSpecializations() {
        return specializations;
    }

    public List<String> getExistingSkills() {
        return existingSkills;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public PreferredLanguage getPreferredLanguage() {
        return preferredLanguage;
    }

    public ProgrammingLevel getProgrammingLevel() {
        return programmingLevel;
    }

    public List<StudyDay> getStudyDays() {
        return studyDays;
    }

    public int getDailyMinutes() {
        return dailyMinutes;
    }

    public LearningStyle getLearningStyle() {
        return learningStyle;
    }

    public ProfileStatus getProfileStatus() {
        return profileStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
