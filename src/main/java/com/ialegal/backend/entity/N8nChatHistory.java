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
@Table(name = "n8n_chat_histories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class N8nChatHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "response", columnDefinition = "TEXT")
    private String response;

    @Column(name = "is_user", nullable = false)
    @Builder.Default
    private Boolean isUser = true;

    @CreatedDate
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "agent_type")
    private String agentType;

    // Campos específicos de pgvector/embeddings (si existen)
    @Column(name = "embedding", columnDefinition = "vector")
    private String embedding; // pgvector field

    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "conversation_id")
    private String conversationId; // Para agrupar mensajes de una conversación

    @Column(name = "message_type")
    private String messageType; // 'user', 'assistant', 'system'

    @Column(name = "model_used")
    private String modelUsed;

    @Column(name = "temperature")
    private Float temperature;

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
        if (this.isUser == null) {
            this.isUser = true;
        }
        if (this.messageType == null) {
            this.messageType = this.isUser ? "user" : "assistant";
        }
    }

    // Métodos de utilidad para compatibilidad con ChatMessage
    public String getContent() {
        return this.message;
    }

    public void setContent(String content) {
        this.message = content;
    }

    public LocalDateTime getCreatedAt() {
        return this.timestamp;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.timestamp = createdAt;
    }

    public String getAgentResponse() {
        return this.response;
    }

    public void setAgentResponse(String agentResponse) {
        this.response = agentResponse;
    }
}