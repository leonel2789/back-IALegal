package com.ialegal.backend.dto;

import com.ialegal.backend.entity.ChatSession;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDto {
    private String sessionId;
    private String userId;
    private String agentType;
    private String sessionName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer messageCount;
    private Boolean isActive;
    private List<MessageDto> messages;

    // Factory method para convertir desde entity
    public static SessionDto fromEntity(ChatSession session) {
        return SessionDto.builder()
                .sessionId(session.getSessionId())
                .userId(session.getUserId())
                .agentType(session.getAgentType().getValue())
                .sessionName(session.getSessionName())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .messageCount(session.getMessageCount())
                .isActive(session.getIsActive())
                .build();
    }

    // Factory method para convertir desde entity con mensajes
    public static SessionDto fromEntityWithMessages(ChatSession session) {
        List<MessageDto> messageDtos = session.getMessages().stream()
                .map(MessageDto::fromEntity)
                .toList();

        return SessionDto.builder()
                .sessionId(session.getSessionId())
                .userId(session.getUserId())
                .agentType(session.getAgentType().getValue())
                .sessionName(session.getSessionName())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .messageCount(session.getMessageCount())
                .isActive(session.getIsActive())
                .messages(messageDtos)
                .build();
    }
}