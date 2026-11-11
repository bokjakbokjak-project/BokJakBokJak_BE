package kr.bokjakbokjak.survey;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.Objects;
import org.springframework.data.domain.Persistable;

/** 사용자의 문항별 최신 답변. */
@Entity
@Table(name = "survey_answer")
public class SurveyAnswer implements Persistable<SurveyAnswer.Id> {
    @EmbeddedId
    private Id id;

    @Column(name = "option_code", nullable = false)
    private String optionCode;

    // 저장할 때마다 전체를 지우고 새로 넣기 때문에, 새로 만든 객체는 merge 없이 바로 insert 한다
    @Transient
    private boolean isNew = true;

    protected SurveyAnswer() {
    }

    public SurveyAnswer(Long userId, String questionCode, String optionCode) {
        this.id = new Id(userId, questionCode);
        this.optionCode = optionCode;
    }

    public String getQuestionCode() {
        return id.questionCode;
    }

    public String getOptionCode() {
        return optionCode;
    }

    @Override
    public Id getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        isNew = false;
    }

    @Embeddable
    public static class Id implements Serializable {
        @Column(name = "user_id")
        private Long userId;

        @Column(name = "question_code")
        private String questionCode;

        protected Id() {
        }

        Id(Long userId, String questionCode) {
            this.userId = userId;
            this.questionCode = questionCode;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Id that
                    && Objects.equals(userId, that.userId)
                    && Objects.equals(questionCode, that.questionCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, questionCode);
        }
    }
}
