package com.ialegal.backend.service;

import com.ialegal.backend.config.AgentRoutingDataSource;
import com.ialegal.backend.dto.*;
import com.ialegal.backend.entity.N8nChatHistory;
import com.ialegal.backend.repository.N8nChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class N8nSessionService {

    private final N8nChatHistoryRepository chatHistoryRepository;

    /**
     * Crear nueva sesión (realmente se crea con el primer mensaje)
     */
    @Transactional
    public SessionDto createSession(String userId, CreateSessionRequest request) {
        log.info("Creating new session for user: {} with agent: {}", userId, request.getAgentType());

        // Establecer datasource según el agente
        AgentRoutingDataSource.setCurrentAgentType(request.getAgentType());

        try {
            // Generar sessionId único
            String sessionId = generateSessionId(userId, request.getAgentType());

            // Si hay primer mensaje, crearlo
            if (request.getFirstMessage() != null && !request.getFirstMessage().trim().isEmpty()) {
                N8nChatHistory firstMessage = N8nChatHistory.builder()
                        .sessionId(sessionId)
                        .userId(userId)
                        .message(request.getFirstMessage().trim())
                        .isUser(true)
                        .agentType(request.getAgentType())
                        .conversationId(sessionId) // Usar sessionId como conversationId
                        .timestamp(LocalDateTime.now())
                        .build();

                chatHistoryRepository.save(firstMessage);
            }

            // Retornar información de la sesión
            String sessionName = request.getSessionName();
            if (sessionName == null || sessionName.trim().isEmpty()) {
                sessionName = request.getFirstMessage() != null ?
                    generateSessionName(request.getFirstMessage()) : "Nueva conversación";
            }

            return SessionDto.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .agentType(request.getAgentType())
                    .sessionName(sessionName)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .messageCount(request.getFirstMessage() != null ? 1 : 0)
                    .isActive(true)
                    .build();

        } finally {
            AgentRoutingDataSource.clearCurrentAgentType();
        }
    }

    /**
     * Obtener sesiones de un usuario por agente
     */
    @Transactional(readOnly = true)
    public List<SessionDto> getUserSessionsByAgent(String userId, String agentType) {
        log.debug("Getting sessions for user: {} and agent: {}", userId, agentType);

        AgentRoutingDataSource.setCurrentAgentType(agentType);

        try {
            // Obtener resúmenes de sesiones agrupadas
            List<Object[]> sessionSummaries = chatHistoryRepository
                    .findSessionSummariesByUserIdAndAgentType(userId, agentType);

            return sessionSummaries.stream()
                    .map(this::convertSummaryToSessionDto)
                    .collect(Collectors.toList());

        } finally {
            AgentRoutingDataSource.clearCurrentAgentType();
        }
    }

    /**
     * Obtener sesión específica con mensajes
     */
    @Transactional(readOnly = true)
    public SessionDto getSession(String sessionId, String userId, String agentType) {
        log.debug("Getting session: {} for user: {}", sessionId, userId);

        AgentRoutingDataSource.setCurrentAgentType(agentType);

        try {
            // Verificar que la sesión pertenece al usuario
            if (!chatHistoryRepository.existsBySessionIdAndUserId(sessionId, userId)) {
                throw new RuntimeException("Session not found or access denied: " + sessionId);
            }

            // Obtener mensajes de la sesión
            List<N8nChatHistory> messages = chatHistoryRepository.findBySessionIdOrderByTimestampAsc(sessionId);

            if (messages.isEmpty()) {
                throw new RuntimeException("Session not found: " + sessionId);
            }

            // Convertir a DTOs
            List<MessageDto> messageDtos = messages.stream()
                    .map(this::convertHistoryToMessageDto)
                    .collect(Collectors.toList());

            // Crear SessionDto
            N8nChatHistory firstMessage = messages.get(0);
            String sessionName = generateSessionName(firstMessage.getMessage());

            return SessionDto.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .agentType(agentType)
                    .sessionName(sessionName)
                    .createdAt(firstMessage.getTimestamp())
                    .updatedAt(messages.get(messages.size() - 1).getTimestamp())
                    .messageCount(messages.size())
                    .isActive(true)
                    .messages(messageDtos)
                    .build();

        } finally {
            AgentRoutingDataSource.clearCurrentAgentType();
        }
    }

    /**
     * Obtener mensajes de una sesión
     */
    @Transactional(readOnly = true)
    public List<MessageDto> getSessionMessages(String sessionId, String userId, String agentType) {
        log.debug("Getting messages for session: {}", sessionId);

        AgentRoutingDataSource.setCurrentAgentType(agentType);

        try {
            // Verificar acceso
            if (!chatHistoryRepository.existsBySessionIdAndUserId(sessionId, userId)) {
                throw new RuntimeException("Session not found or access denied: " + sessionId);
            }

            List<N8nChatHistory> messages = chatHistoryRepository.findBySessionIdOrderByTimestampAsc(sessionId);
            return messages.stream()
                    .map(this::convertHistoryToMessageDto)
                    .collect(Collectors.toList());

        } finally {
            AgentRoutingDataSource.clearCurrentAgentType();
        }
    }

    /**
     * Agregar mensaje a sesión
     */
    @Transactional
    public MessageDto addMessage(String sessionId, String userId, String agentType, AddMessageRequest request) {
        log.debug("Adding message to session: {}", sessionId);

        AgentRoutingDataSource.setCurrentAgentType(agentType);

        try {
            // Verificar que la sesión existe y pertenece al usuario
            if (!chatHistoryRepository.existsBySessionIdAndUserId(sessionId, userId)) {
                throw new RuntimeException("Session not found or access denied: " + sessionId);
            }

            // Crear nuevo mensaje
            N8nChatHistory message = N8nChatHistory.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .message(request.getContent())
                    .response(request.getAgentResponse())
                    .isUser(request.getIsUser())
                    .agentType(agentType)
                    .conversationId(sessionId)
                    .processingTimeMs(request.getProcessingTimeMs())
                    .errorMessage(request.getErrorMessage())
                    .metadata(request.getMetadata())
                    .timestamp(LocalDateTime.now())
                    .build();

            N8nChatHistory savedMessage = chatHistoryRepository.save(message);
            return convertHistoryToMessageDto(savedMessage);

        } finally {
            AgentRoutingDataSource.clearCurrentAgentType();
        }
    }

    /**
     * Buscar sesiones por contenido
     */
    @Transactional(readOnly = true)
    public List<SessionDto> searchSessions(String userId, String agentType, String searchTerm) {
        log.debug("Searching sessions for user: {} with term: {}", userId, searchTerm);

        AgentRoutingDataSource.setCurrentAgentType(agentType);

        try {
            List<N8nChatHistory> messages = chatHistoryRepository
                    .findMessagesByUserAndContentContaining(userId, searchTerm);

            // Agrupar por sessionId y convertir a SessionDto
            return messages.stream()
                    .collect(Collectors.groupingBy(N8nChatHistory::getSessionId))
                    .entrySet().stream()
                    .map(entry -> {
                        List<N8nChatHistory> sessionMessages = entry.getValue();
                        N8nChatHistory firstMessage = sessionMessages.get(0);

                        return SessionDto.builder()
                                .sessionId(entry.getKey())
                                .userId(userId)
                                .agentType(agentType)
                                .sessionName(generateSessionName(firstMessage.getMessage()))
                                .createdAt(firstMessage.getTimestamp())
                                .updatedAt(sessionMessages.stream()
                                        .map(N8nChatHistory::getTimestamp)
                                        .max(LocalDateTime::compareTo)
                                        .orElse(firstMessage.getTimestamp()))
                                .messageCount(sessionMessages.size())
                                .isActive(true)
                                .build();
                    })
                    .collect(Collectors.toList());

        } finally {
            AgentRoutingDataSource.clearCurrentAgentType();
        }
    }

    // Métodos de utilidad privados

    private String generateSessionId(String userId, String agentType) {
        return String.format("%s_%s_%d_%s",
                userId,
                agentType,
                System.currentTimeMillis(),
                UUID.randomUUID().toString().substring(0, 8));
    }

    private String generateSessionName(String firstMessage) {
        if (firstMessage == null || firstMessage.trim().isEmpty()) {
            return "Nueva conversación";
        }

        String[] words = firstMessage.trim().split("\\s+");
        StringBuilder name = new StringBuilder();

        for (int i = 0; i < Math.min(4, words.length); i++) {
            if (i > 0) name.append(" ");
            name.append(words[i]);
        }

        String result = name.toString();
        if (firstMessage.length() > 30) {
            result += "...";
        }

        return result.length() > 50 ? result.substring(0, 47) + "..." : result;
    }

    private SessionDto convertSummaryToSessionDto(Object[] summary) {
        String sessionId = (String) summary[0];
        LocalDateTime firstMessage = (LocalDateTime) summary[1];
        LocalDateTime lastMessage = (LocalDateTime) summary[2];
        Long messageCount = (Long) summary[3];

        // Obtener primer mensaje para generar el nombre
        N8nChatHistory firstUserMessage = chatHistoryRepository.findFirstUserMessageBySessionId(sessionId);
        String sessionName = firstUserMessage != null ?
                generateSessionName(firstUserMessage.getMessage()) : "Conversación";

        return SessionDto.builder()
                .sessionId(sessionId)
                .sessionName(sessionName)
                .createdAt(firstMessage)
                .updatedAt(lastMessage)
                .messageCount(messageCount.intValue())
                .isActive(true)
                .build();
    }

    private MessageDto convertHistoryToMessageDto(N8nChatHistory history) {
        return MessageDto.builder()
                .id(history.getId())
                .sessionId(history.getSessionId())
                .content(history.getMessage())
                .isUser(history.getIsUser())
                .createdAt(history.getTimestamp())
                .agentResponse(history.getResponse())
                .processingTimeMs(history.getProcessingTimeMs())
                .errorMessage(history.getErrorMessage())
                .metadata(history.getMetadata())
                .build();
    }
}