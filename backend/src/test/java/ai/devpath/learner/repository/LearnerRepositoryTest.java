package ai.devpath.learner.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ai.devpath.learner.model.Learner;
import ai.devpath.learner.model.ExperienceBand;
import ai.devpath.learner.model.LearningStyle;
import ai.devpath.learner.model.PreferredLanguage;
import ai.devpath.learner.model.ProfileStatus;
import ai.devpath.learner.model.ProgrammingLevel;
import ai.devpath.learner.model.StudyDay;
import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class LearnerRepositoryTest {
    @Autowired
    private LearnerRepository learnerRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsAndReadsJsonbSkillsAndStudyDays() {
        Learner learner = new Learner(
                "QA Engineer",
                ExperienceBand.FOUR_TO_FIVE,
                List.of("API Testing", "Database Testing"),
                List.of("Postman", "SQL"),
                "Java Backend Developer",
                PreferredLanguage.JAVA,
                ProgrammingLevel.BEGINNER,
                List.of(StudyDay.MONDAY, StudyDay.WEDNESDAY, StudyDay.SATURDAY),
                45,
                LearningStyle.PRACTICAL,
                ProfileStatus.CREATED);

        Learner saved = learnerRepository.saveAndFlush(learner);
        entityManager.clear();

        Learner loaded = learnerRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getExperienceBand()).isEqualTo(ExperienceBand.FOUR_TO_FIVE);
        assertThat(loaded.getSpecializations()).containsExactly("API Testing", "Database Testing");
        assertThat(loaded.getExistingSkills()).containsExactly("Postman", "SQL");
        assertThat(loaded.getStudyDays())
                .containsExactly(StudyDay.MONDAY, StudyDay.WEDNESDAY, StudyDay.SATURDAY);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();
    }
}
