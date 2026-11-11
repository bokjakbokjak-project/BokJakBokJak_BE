package kr.bokjakbokjak.survey;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "question")
public class Question {
    @Id
    private String code;

    @Column(name = "topic_code", nullable = false)
    private String topicCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionKind kind;

    @Column(name = "question_number", nullable = false)
    private int questionNumber;

    @Column(nullable = false)
    private String prompt;

    protected Question() {
    }

    public String getCode() {
        return code;
    }

    public String getTopicCode() {
        return topicCode;
    }

    public QuestionKind getKind() {
        return kind;
    }

    public int getQuestionNumber() {
        return questionNumber;
    }

    public String getPrompt() {
        return prompt;
    }
}
