package kr.bokjakbokjak.room.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "chat_message", indexes = @Index(
        name = "idx_chat_message_matching", columnList = "matching_id, chat_message_id"))
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_message_id")
    private Long id;

    @Column(name = "matching_id", nullable = false)
    private Long matchingId;

    @Column(name = "sender_id")
    private Long senderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private MessageType type;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "agenda_id")
    private Long agendaId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ChatMessage() {
    }

    private ChatMessage(Long matchingId, Long senderId, MessageType type, String content, Long agendaId) {
        this.matchingId = matchingId;
        this.senderId = senderId;
        this.type = type;
        this.content = content;
        this.agendaId = agendaId;
    }

    public static ChatMessage user(Long matchingId, Long senderId, String content, Long agendaId) {
        return new ChatMessage(matchingId, senderId, MessageType.USER, content, agendaId);
    }

    public static ChatMessage ai(Long matchingId, String content, Long agendaId) {
        return new ChatMessage(matchingId, null, MessageType.AI, content, agendaId);
    }

    public static ChatMessage system(Long matchingId, String content, Long agendaId) {
        return new ChatMessage(matchingId, null, MessageType.SYSTEM, content, agendaId);
    }

    public Long getId() {
        return id;
    }

    public Long getMatchingId() {
        return matchingId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public MessageType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public Long getAgendaId() {
        return agendaId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
