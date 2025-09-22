package com.ialegal.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ChatSession {

    @Id
    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "agent_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private AgentType agentType;

    @Column(name = "session_name", nullable = false, length = 200)
    private String sessionName;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "message_count")
    @Builder.Default
    private Integer messageCount = 0;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    // Enum para tipos de agente
    public enum AgentType {
        IA_CONTRATOS("ia-contratos"),
        IA_LABORAL("ia-laboral"),
        IA_DEFENSA_CONSUMIDOR("ia-defensa-consumidor"),
        IA_GENERAL("ia-general");

        private final String value;

        AgentType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static AgentType fromValue(String value) {
            for (AgentType type : AgentType.values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown agent type: " + value);
        }
    }

    // Métodos de utilidad
    public void incrementMessageCount() {
        this.messageCount = (this.messageCount == null ? 0 : this.messageCount) + 1;
    }

    public void addMessage(ChatMessage message) {
        this.messages.add(message);
        message.setSession(this);
        incrementMessageCount();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.messageCount == null) {
            this.messageCount = 0;
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}