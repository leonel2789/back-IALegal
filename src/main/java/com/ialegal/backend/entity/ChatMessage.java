package com.ialegal.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", referencedColumnName = "session_id", nullable = false)
    private ChatSession session;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_user", nullable = false)
    private Boolean isUser;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "message_order")
    private Integer messageOrder;

    @Column(name = "agent_response", columnDefinition = "TEXT")
    private String agentResponse;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON string for additional data

    // Método para obtener el sessionId directamente
    public String getSessionId() {
        return session != null ? session.getSessionId() : null;
    }

    // Método para obtener el agentType directamente
    public ChatSession.AgentType getAgentType() {
        return session != null ? session.getAgentType() : null;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }

        // Auto-incrementar el order dentro de la sesión
        if (this.messageOrder == null && this.session != null) {
            this.messageOrder = this.session.getMessageCount() + 1;
        }
    }

    // Builder helper methods
    public static ChatMessage userMessage(ChatSession session, String content) {
        return ChatMessage.builder()
                .session(session)
                .content(content)
                .isUser(true)
                .build();
    }

    public static ChatMessage agentMessage(ChatSession session, String content, String agentResponse) {
        return ChatMessage.builder()
                .session(session)
                .content(content)
                .agentResponse(agentResponse)
                .isUser(false)
                .build();
    }
}