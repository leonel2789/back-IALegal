package com.ialegal.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSessionRequest {

    @NotBlank(message = "User ID is required")
    @Size(max = 100, message = "User ID must not exceed 100 characters")
    private String userId;

    @NotBlank(message = "Agent type is required")
    private String agentType;

    @Size(max = 200, message = "Session name must not exceed 200 characters")
    private String sessionName;

    @Size(max = 1000, message = "First message must not exceed 1000 characters")
    private String firstMessage;
}