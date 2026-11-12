package kr.bokjakbokjak.university.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 학교 Google 계정의 hd 또는 이메일 도메인. */
@Entity
@Table(name = "university_domain")
public class UniversityDomain {
    @Id
    private String domain;

    @Column(name = "university_id", nullable = false)
    private Long universityId;

    protected UniversityDomain() {
    }

    public String getDomain() {
        return domain;
    }

    public Long getUniversityId() {
        return universityId;
    }
}
