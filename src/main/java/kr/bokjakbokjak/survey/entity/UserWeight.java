package kr.bokjakbokjak.survey.entity;

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

/** 사용자가 고른 생활습관 카테고리와 순위 (3~5개). */
@Entity
@Table(name = "user_weight")
public class UserWeight implements Persistable<UserWeight.Id> {
    @EmbeddedId
    private Id id;

    @Column(name = "priority_rank", nullable = false)
    private int rank;

    // 저장할 때마다 전체를 지우고 새로 넣기 때문에, 새로 만든 객체는 merge 없이 바로 insert 한다
    @Transient
    private boolean isNew = true;

    protected UserWeight() {
    }

    public UserWeight(Long userId, String categoryCode, int rank) {
        this.id = new Id(userId, categoryCode);
        this.rank = rank;
    }

    public String getCategoryCode() {
        return id.categoryCode;
    }

    public int getRank() {
        return rank;
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

        @Column(name = "category_code")
        private String categoryCode;

        protected Id() {
        }

        Id(Long userId, String categoryCode) {
            this.userId = userId;
            this.categoryCode = categoryCode;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Id that
                    && Objects.equals(userId, that.userId)
                    && Objects.equals(categoryCode, that.categoryCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, categoryCode);
        }
    }
}
