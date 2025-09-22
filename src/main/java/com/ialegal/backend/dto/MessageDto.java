package com.ialegal.backend.dto;

import com.ialegal.backend.entity.ChatMessage;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {
    private Long id;
    private String sessionId;
    private String content;
    private Boolean isUser;
    private LocalDateTime createdAt;
    private Integer messageOrder;
    private String agentResponse;
    private Long processingTimeMs;
    private String errorMessage;
    private String metadata;

    // Factory method para convertir desde entity
    public static MessageDto fromEntity(ChatMessage message) {
        return MessageDto.builder()
                .id(message.getId())
                .sessionId(message.getSessionId())
                .content(message.getContent())
                .isUser(message.getIsUser())
                .createdAt(message.getCreatedAt())
                .messageOrder(message.getMessageOrder())
                .agentResponse(message.getAgentResponse())
                .processingTimeMs(message.getProcessingTimeMs())
                .errorMessage(message.getErrorMessage())
                .metadata(message.getMetadata())
                .build();
    }
}