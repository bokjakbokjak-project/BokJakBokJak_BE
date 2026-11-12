package kr.bokjakbokjak.room.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import kr.bokjakbokjak.common.BaseEntity;

@Entity
@Table(name = "agenda", uniqueConstraints = @UniqueConstraint(
        name = "uk_agenda_matching_seq", columnNames = {"matching_id", "seq"}))
public class Agenda extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agenda_id")
    private Long id;

    @Column(name = "matching_id", nullable = false)
    private Long matchingId;

    @Column(name = "seq", nullable = false)
    private short seq;

    @Column(name = "topic_code", length = 40)
    private String topicCode;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AgendaStatus status;

    @Column(name = "decided_by")
    private Long decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "rule_summary", length = 255)
    private String ruleSummary;

    protected Agenda() {
    }

    private Agenda(Long matchingId, int seq, String topicCode, String title, String content, AgendaStatus status) {
        this.matchingId = matchingId;
        this.seq = (short) seq;
        this.topicCode = topicCode;
        this.title = title;
        this.content = content;
        this.status = status;
    }

    public static Agenda create(Long matchingId, int seq, String topicCode, String title, String content,
            AgendaStatus status) {
        return new Agenda(matchingId, seq, topicCode, title, content, status);
    }

    public void start() {
        if (status == AgendaStatus.WAITING) {
            status = AgendaStatus.DISCUSSING;
        }
    }

    public boolean isWaiting() {
        return status == AgendaStatus.WAITING;
    }

    public boolean isDecided() {
        return status == AgendaStatus.DECIDED;
    }

    public Long getId() {
        return id;
    }

    public Long getMatchingId() {
        return matchingId;
    }

    public int getSeq() {
        return seq;
    }

    public String getTopicCode() {
        return topicCode;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public AgendaStatus getStatus() {
        return status;
    }

    public Long getDecidedBy() {
        return decidedBy;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public String getRuleSummary() {
        return ruleSummary;
    }
}
