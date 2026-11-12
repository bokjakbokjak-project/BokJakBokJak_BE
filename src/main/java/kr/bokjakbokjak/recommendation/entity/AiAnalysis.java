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
@Table(name = "ai_analysis", uniqueConstraints = @UniqueConstraint(
        name = "uk_ai_analysis_viewer_target", columnNames = {"viewer_id", "target_id"}))
public class AiAnalysis extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_analysis_id")
    private Long id;

    @Column(name = "viewer_id", nullable = false)
    private Long viewerId;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "viewer_survey_revision", nullable = false)
    private int viewerSurveyRevision;

    @Column(name = "target_survey_revision", nullable = false)
    private int targetSurveyRevision;

    @Column(name = "good_points", nullable = false, columnDefinition = "TEXT")
    private String goodPoints;

    @Column(name = "adjust_points", nullable = false, columnDefinition = "TEXT")
    private String adjustPoints;

    @Column(name = "check_points", nullable = false, columnDefinition = "TEXT")
    private String checkPoints;

    protected AiAnalysis() {
    }

    private AiAnalysis(Long viewerId, Long targetId, int viewerSurveyRevision, int targetSurveyRevision,
            String goodPoints, String adjustPoints, String checkPoints) {
        this.viewerId = viewerId;
        this.targetId = targetId;
        this.viewerSurveyRevision = viewerSurveyRevision;
        this.targetSurveyRevision = targetSurveyRevision;
        this.goodPoints = goodPoints;
        this.adjustPoints = adjustPoints;
        this.checkPoints = checkPoints;
    }

    public static AiAnalysis create(Long viewerId, Long targetId, int viewerSurveyRevision,
            int targetSurveyRevision, String goodPoints, String adjustPoints, String checkPoints) {
        return new AiAnalysis(viewerId, targetId, viewerSurveyRevision, targetSurveyRevision,
                goodPoints, adjustPoints, checkPoints);
    }

    public boolean isFor(int viewerSurveyRevision, int targetSurveyRevision) {
        return this.viewerSurveyRevision == viewerSurveyRevision && this.targetSurveyRevision == targetSurveyRevision;
    }

    public void refresh(int viewerSurveyRevision, int targetSurveyRevision, String goodPoints, String adjustPoints,
            String checkPoints) {
        this.viewerSurveyRevision = viewerSurveyRevision;
        this.targetSurveyRevision = targetSurveyRevision;
        this.goodPoints = goodPoints;
        this.adjustPoints = adjustPoints;
        this.checkPoints = checkPoints;
    }

    public Long getId() {
        return id;
    }

    public Long getViewerId() {
        return viewerId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public int getViewerSurveyRevision() {
        return viewerSurveyRevision;
    }

    public int getTargetSurveyRevision() {
        return targetSurveyRevision;
    }

    public String getGoodPoints() {
        return goodPoints;
    }

    public String getAdjustPoints() {
        return adjustPoints;
    }

    public String getCheckPoints() {
        return checkPoints;
    }
}
