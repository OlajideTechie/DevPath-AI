package ai.devpath.learner.dto;

import java.util.List;

import org.hibernate.validator.constraints.UniqueElements;

import ai.devpath.learner.model.LearningStyle;
import ai.devpath.learner.model.ProgrammingLevel;
import ai.devpath.learner.model.StudyDay;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateLearnerRequest(
        @NotBlank @Size(max = 100) String currentRole,
        @NotNull @Min(value = 0, message = "must be between 0 and 50")
        @Max(value = 50, message = "must be between 0 and 50") Integer yearsOfExperience,
        @NotBlank @Size(max = 100) String specialization,
        @NotNull @Size(max = 20) List<@NotBlank @Size(max = 100) String> existingSkills,
        @NotNull ProgrammingLevel programmingLevel,
        @NotEmpty @UniqueElements List<@NotNull StudyDay> studyDays,
        @NotNull @Min(value = 10, message = "must be between 10 and 240")
        @Max(value = 240, message = "must be between 10 and 240") Integer dailyMinutes,
        @NotNull LearningStyle learningStyle) {
}
