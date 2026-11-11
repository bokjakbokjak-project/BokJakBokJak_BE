package kr.bokjakbokjak.survey;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "survey_topic")
public class SurveyTopic {
    @Id
    private String code;

    @Column(name = "category_code", nullable = false)
    private String categoryCode;

    @Column(name = "scenario_text", nullable = false)
    private String scenarioText;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected SurveyTopic() {
    }

    public String getCode() {
        return code;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public String getScenarioText() {
        return scenarioText;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
