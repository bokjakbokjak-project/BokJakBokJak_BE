package kr.bokjakbokjak.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import kr.bokjakbokjak.common.BaseEntity;

@Entity
@Table(name = "app_user")
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "onboarding_completed_at")
    private Instant onboardingCompletedAt;

    @Column(name = "survey_revision", nullable = false)
    private int surveyRevision;

    @Column(name = "survey_completed_at")
    private Instant surveyCompletedAt;

    protected User() {
    }

    public boolean isOnboardingCompleted() {
        return onboardingCompletedAt != null;
    }

    public boolean isSurveyCompleted() {
        return surveyRevision > 0;
    }

    /** 설문 내용이 바뀌었을 때 호출한다. 차수를 1 올리고 새 차수를 돌려준다. */
    public int completeSurvey(Instant submittedAt) {
        surveyRevision++;
        surveyCompletedAt = submittedAt;
        return surveyRevision;
    }

    public Long getId() {
        return id;
    }

    public Instant getOnboardingCompletedAt() {
        return onboardingCompletedAt;
    }

    public int getSurveyRevision() {
        return surveyRevision;
    }

    public Instant getSurveyCompletedAt() {
        return surveyCompletedAt;
    }
}
