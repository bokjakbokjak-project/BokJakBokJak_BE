package kr.bokjakbokjak.survey.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "question_option")
public class QuestionOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_code", nullable = false)
    private String questionCode;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String label;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected QuestionOption() {
    }

    public Long getId() {
        return id;
    }

    public String getQuestionCode() {
        return questionCode;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
