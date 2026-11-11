package kr.bokjakbokjak.recommendation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kr.bokjakbokjak.common.BaseEntity;

@Entity
@Table(name = "lifestyle_summary", uniqueConstraints = @UniqueConstraint(
        name = "uk_lifestyle_summary_user", columnNames = "user_id"))
public class LifestyleSummary extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lifestyle_summary_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "survey_revision", nullable = false)
    private int surveyRevision;

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "keywords", nullable = false, columnDefinition = "TEXT")
    private String keywords;

    protected LifestyleSummary() {
    }

    private LifestyleSummary(Long userId, int surveyRevision, String summary, String keywords) {
        this.userId = userId;
        this.surveyRevision = surveyRevision;
        this.summary = summary;
        this.keywords = keywords;
    }

    public static LifestyleSummary create(Long userId, int surveyRevision, String summary, String keywords) {
        return new LifestyleSummary(userId, surveyRevision, summary, keywords);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public int getSurveyRevision() {
        return surveyRevision;
    }

    public String getSummary() {
        return summary;
    }

    public String getKeywords() {
        return keywords;
    }
}
