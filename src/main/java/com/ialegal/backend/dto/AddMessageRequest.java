package com.ialegal.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddMessageRequest {

    @NotBlank(message = "Content is required")
    @Size(max = 10000, message = "Content must not exceed 10000 characters")
    private String content;

    @NotNull(message = "isUser flag is required")
    private Boolean isUser;

    @Size(max = 10000, message = "Agent response must not exceed 10000 characters")
    private String agentResponse;

    @Size(max = 1000, message = "Error message must not exceed 1000 characters")
    private String errorMessage;

    @Size(max = 2000, message = "Metadata must not exceed 2000 characters")
    private String metadata;

    private Long processingTimeMs;
}