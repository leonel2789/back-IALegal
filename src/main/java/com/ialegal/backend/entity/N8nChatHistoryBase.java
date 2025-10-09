package com.ialegal.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Clase base abstracta para las entidades de historial de chat de N8N.
 * Mapea la estructura real de N8N: id, session_id, message (JSONB)
 */
@MappedSuperclass
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class N8nChatHistoryBase {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "message", nullable = false, columnDefinition = "JSONB")
    private String message; // JSONB raw string

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Clase interna para representar el contenido del campo JSONB message
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessageContent {
        @JsonProperty("type")
        private String type; // "human" o "ai"

        @JsonProperty("content")
        private String content; // El texto del mensaje

        @JsonProperty("additional_kwargs")
        private Map<String, Object> additionalKwargs;

        @JsonProperty("response_metadata")
        private Map<String, Object> responseMetadata;
    }

    // Métodos de utilidad para parsear el JSONB

    public MessageContent getParsedMessage() {
        if (this.message == null) {
            return null;
        }
        try {
            return objectMapper.readValue(this.message, MessageContent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error parsing message JSON: " + e.getMessage(), e);
        }
    }

    public String getContent() {
        MessageContent parsed = getParsedMessage();
        return parsed != null ? parsed.getContent() : null;
    }

    public boolean isUserMessage() {
        MessageContent parsed = getParsedMessage();
        return parsed != null && "human".equals(parsed.getType());
    }

    public boolean isAiMessage() {
        MessageContent parsed = getParsedMessage();
        return parsed != null && "ai".equals(parsed.getType());
    }

    // Extraer userId del sessionId (formato: userId_agentType_timestamp_uuid)
    public String getUserId() {
        if (this.sessionId == null) {
            return null;
        }
        String[] parts = this.sessionId.split("_");
        return parts.length > 0 ? parts[0] : null;
    }

    // Extraer agentType del sessionId
    public String getAgentType() {
        if (this.sessionId == null) {
            return null;
        }
        String[] parts = this.sessionId.split("_");
        return parts.length > 1 ? parts[1] : null;
    }
}
