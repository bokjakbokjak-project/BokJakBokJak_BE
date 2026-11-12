package kr.bokjakbokjak.room.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import kr.bokjakbokjak.common.BaseEntity;

@Entity
@Table(name = "matching",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_matching_user_pair", columnNames = {"user_low_id", "user_high_id"}),
        indexes = {
                @Index(name = "idx_matching_requester_status", columnList = "requester_id, status"),
                @Index(name = "idx_matching_receiver_status", columnList = "receiver_id, status")
        })
public class Matching extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "matching_id")
    private Long id;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "user_low_id", nullable = false)
    private Long userLowId;

    @Column(name = "user_high_id", nullable = false)
    private Long userHighId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MatchingStatus status;

    @Column(name = "requester_confirmed", nullable = false)
    private boolean requesterConfirmed;

    @Column(name = "receiver_confirmed", nullable = false)
    private boolean receiverConfirmed;

    @Column(name = "ended_by")
    private Long endedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "end_reason", length = 20)
    private EndReason endReason;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "requester_survey_revision", nullable = false)
    private int requesterSurveyRevision;

    @Column(name = "receiver_survey_revision", nullable = false)
    private int receiverSurveyRevision;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Matching() {
    }

    private Matching(Long requesterId, Long receiverId, int requesterSurveyRevision, int receiverSurveyRevision) {
        this.requesterId = requesterId;
        this.receiverId = receiverId;
        this.userLowId = Math.min(requesterId, receiverId);
        this.userHighId = Math.max(requesterId, receiverId);
        this.status = MatchingStatus.CHATTING;
        this.requesterSurveyRevision = requesterSurveyRevision;
        this.receiverSurveyRevision = receiverSurveyRevision;
    }

    public static Matching start(Long requesterId, Long receiverId,
            int requesterSurveyRevision, int receiverSurveyRevision) {
        return new Matching(requesterId, receiverId, requesterSurveyRevision, receiverSurveyRevision);
    }

    public boolean isParticipant(Long userId) {
        return requesterId.equals(userId) || receiverId.equals(userId);
    }

    public Long partnerOf(Long userId) {
        if (requesterId.equals(userId)) {
            return receiverId;
        }
        if (receiverId.equals(userId)) {
            return requesterId;
        }
        throw new IllegalArgumentException("Not a participant: " + userId);
    }

    public boolean isConfirmedBy(Long userId) {
        if (requesterId.equals(userId)) {
            return requesterConfirmed;
        }
        if (receiverId.equals(userId)) {
            return receiverConfirmed;
        }
        throw new IllegalArgumentException("Not a participant: " + userId);
    }

    public void endBy(Long userId) {
        end(userId, EndReason.USER_ENDED);
    }

    public void endByPartnerMatched() {
        end(null, EndReason.PARTNER_MATCHED);
    }

    private void end(Long endedBy, EndReason reason) {
        if (!isChatting()) {
            throw new IllegalStateException("Only chatting matching can end: " + id);
        }
        this.status = MatchingStatus.ENDED;
        this.endedBy = endedBy;
        this.endReason = reason;
        this.endedAt = Instant.now();
    }

    public boolean isChatting() {
        return status == MatchingStatus.CHATTING;
    }

    public boolean isEnded() {
        return status == MatchingStatus.ENDED;
    }

    public Long getId() {
        return id;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public Long getUserLowId() {
        return userLowId;
    }

    public Long getUserHighId() {
        return userHighId;
    }

    public MatchingStatus getStatus() {
        return status;
    }

    public boolean isRequesterConfirmed() {
        return requesterConfirmed;
    }

    public boolean isReceiverConfirmed() {
        return receiverConfirmed;
    }

    public Long getEndedBy() {
        return endedBy;
    }

    public EndReason getEndReason() {
        return endReason;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public int getRequesterSurveyRevision() {
        return requesterSurveyRevision;
    }

    public int getReceiverSurveyRevision() {
        return receiverSurveyRevision;
    }

    public Long getVersion() {
        return version;
    }
}
