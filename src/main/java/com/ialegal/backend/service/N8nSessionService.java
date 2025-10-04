package com.ialegal.backend.service;

import com.ialegal.backend.dto.*;
import com.ialegal.backend.entity.*;
import com.ialegal.backend.repository.*;
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

    private final N8nChatHistoryContratosRepository contratosRepository;
    private final N8nChatHistoryLaboralRepository laboralRepository;
    private final N8nChatHistoryDefensaRepository defensaRepository;
    private final N8nChatHistoryGeneralRepository generalRepository;

    /**
     * Obtener el repositorio correcto según el tipo de agente
     */
    @SuppressWarnings("unchecked")
    private <T extends N8nChatHistoryBase> N8nChatHistoryBaseRepository<T> getRepository(String agentType) {
        return switch (agentType) {
            case "ia-contratos" -> (N8nChatHistoryBaseRepository<T>) contratosRepository;
            case "ia-laboral" -> (N8nChatHistoryBaseRepository<T>) laboralRepository;
            case "ia-defensa-consumidor" -> (N8nChatHistoryBaseRepository<T>) defensaRepository;
            case "ia-general" -> (N8nChatHistoryBaseRepository<T>) generalRepository;
            default -> {
                log.warn("Unknown agent type: {}, defaulting to general", agentType);
                yield (N8nChatHistoryBaseRepository<T>) generalRepository;
            }
        };
    }

    /**
     * Crear una nueva instancia de la entidad correcta según el tipo de agente
     */
    private N8nChatHistoryBase createEntity(String agentType) {
        return switch (agentType) {
            case "ia-contratos" -> new N8nChatHistoryContratos();
            case "ia-laboral" -> new N8nChatHistoryLaboral();
            case "ia-defensa-consumidor" -> new N8nChatHistoryDefensa();
            case "ia-general" -> new N8nChatHistoryGeneral();
            default -> {
                log.warn("Unknown agent type: {}, defaulting to general", agentType);
                yield new N8nChatHistoryGeneral();
            }
        };
    }

    /**
     * Crear builder para la entidad correcta según el tipo de agente
     */
    @SuppressWarnings("unchecked")
    private <T extends N8nChatHistoryBase> T buildEntity(String agentType,
                                                          String sessionId,
                                                          String userId,
                                                          String message,
                                                          Boolean isUser,
                                                          String response,
                                                          Long processingTimeMs,
                                                          String errorMessage,
                                                          String metadata) {
        LocalDateTime now = LocalDateTime.now();

        return (T) switch (agentType) {
            case "ia-contratos" -> N8nChatHistoryContratos.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .message(message)
                    .response(response)
                    .isUser(isUser)
                    .agentType(agentType)
                    .conversationId(sessionId)
                    .processingTimeMs(processingTimeMs)
                    .errorMessage(errorMessage)
                    .metadata(metadata)
                    .timestamp(now)
                    .build();
            case "ia-laboral" -> N8nChatHistoryLaboral.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .message(message)
                    .response(response)
                    .isUser(isUser)
                    .agentType(agentType)
                    .conversationId(sessionId)
                    .processingTimeMs(processingTimeMs)
                    .errorMessage(errorMessage)
                    .metadata(metadata)
                    .timestamp(now)
                    .build();
            case "ia-defensa-consumidor" -> N8nChatHistoryDefensa.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .message(message)
                    .response(response)
                    .isUser(isUser)
                    .agentType(agentType)
                    .conversationId(sessionId)
                    .processingTimeMs(processingTimeMs)
                    .errorMessage(errorMessage)
                    .metadata(metadata)
                    .timestamp(now)
                    .build();
            case "ia-general" -> N8nChatHistoryGeneral.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .message(message)
                    .response(response)
                    .isUser(isUser)
                    .agentType(agentType)
                    .conversationId(sessionId)
                    .processingTimeMs(processingTimeMs)
                    .errorMessage(errorMessage)
                    .metadata(metadata)
                    .timestamp(now)
                    .build();
            default -> {
                log.warn("Unknown agent type: {}, defaulting to general", agentType);
                yield (T) N8nChatHistoryGeneral.builder()
                        .sessionId(sessionId)
                        .userId(userId)
                        .message(message)
                        .response(response)
                        .isUser(isUser)
                        .agentType(agentType)
                        .conversationId(sessionId)
                        .processingTimeMs(processingTimeMs)
                        .errorMessage(errorMessage)
                        .metadata(metadata)
                        .timestamp(now)
                        .build();
            }
        };
    }

    /**
     * Crear nueva sesión (realmente se crea con el primer mensaje)
     */
    @Transactional
    public SessionDto createSession(String userId, CreateSessionRequest request) {
        log.info("Creating new session for user: {} with agent: {}", userId, request.getAgentType());

        String agentType = request.getAgentType();
        N8nChatHistoryBaseRepository<N8nChatHistoryBase> repository = getRepository(agentType);

        // Generar sessionId único
        String sessionId = generateSessionId(userId, agentType);

        // Si hay primer mensaje, crearlo
        if (request.getFirstMessage() != null && !request.getFirstMessage().trim().isEmpty()) {
            N8nChatHistoryBase firstMessage = buildEntity(
                    agentType,
                    sessionId,
                    userId,
                    request.getFirstMessage().trim(),
                    true,
                    null,
                    null,
                    null,
                    null
            );

            repository.save(firstMessage);
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
                .agentType(agentType)
                .sessionName(sessionName)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .messageCount(request.getFirstMessage() != null ? 1 : 0)
                .isActive(true)
                .build();
    }

    /**
     * Obtener sesiones de un usuario por agente
     */
    @Transactional(readOnly = true)
    public List<SessionDto> getUserSessionsByAgent(String userId, String agentType) {
        log.debug("Getting sessions for user: {} and agent: {}", userId, agentType);

        N8nChatHistoryBaseRepository<N8nChatHistoryBase> repository = getRepository(agentType);

        // Obtener resúmenes de sesiones agrupadas
        List<Object[]> sessionSummaries = repository
                .findSessionSummariesByUserIdAndAgentType(userId, agentType);

        return sessionSummaries.stream()
                .map(summary -> convertSummaryToSessionDto(summary, agentType))
                .collect(Collectors.toList());
    }

    /**
     * Obtener sesión específica con mensajes
     */
    @Transactional(readOnly = true)
    public SessionDto getSession(String sessionId, String userId, String agentType) {
        log.debug("Getting session: {} for user: {}", sessionId, userId);

        N8nChatHistoryBaseRepository<N8nChatHistoryBase> repository = getRepository(agentType);

        // Verificar que la sesión pertenece al usuario
        if (!repository.existsBySessionIdAndUserId(sessionId, userId)) {
            throw new RuntimeException("Session not found or access denied: " + sessionId);
        }

        // Obtener mensajes de la sesión
        List<N8nChatHistoryBase> messages = repository.findBySessionIdOrderByTimestampAsc(sessionId);

        if (messages.isEmpty()) {
            throw new RuntimeException("Session not found: " + sessionId);
        }

        // Convertir a DTOs
        List<MessageDto> messageDtos = messages.stream()
                .map(this::convertHistoryToMessageDto)
                .collect(Collectors.toList());

        // Crear SessionDto
        N8nChatHistoryBase firstMessage = messages.get(0);
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
    }

    /**
     * Obtener mensajes de una sesión
     */
    @Transactional(readOnly = true)
    public List<MessageDto> getSessionMessages(String sessionId, String userId, String agentType) {
        log.debug("Getting messages for session: {}", sessionId);

        N8nChatHistoryBaseRepository<N8nChatHistoryBase> repository = getRepository(agentType);

        // Verificar acceso
        if (!repository.existsBySessionIdAndUserId(sessionId, userId)) {
            throw new RuntimeException("Session not found or access denied: " + sessionId);
        }

        List<N8nChatHistoryBase> messages = repository.findBySessionIdOrderByTimestampAsc(sessionId);
        return messages.stream()
                .map(this::convertHistoryToMessageDto)
                .collect(Collectors.toList());
    }

    /**
     * Agregar mensaje a sesión
     */
    @Transactional
    public MessageDto addMessage(String sessionId, String userId, String agentType, AddMessageRequest request) {
        log.debug("Adding message to session: {}", sessionId);

        N8nChatHistoryBaseRepository<N8nChatHistoryBase> repository = getRepository(agentType);

        // Verificar que la sesión existe y pertenece al usuario
        if (!repository.existsBySessionIdAndUserId(sessionId, userId)) {
            throw new RuntimeException("Session not found or access denied: " + sessionId);
        }

        // Crear nuevo mensaje
        N8nChatHistoryBase message = buildEntity(
                agentType,
                sessionId,
                userId,
                request.getContent(),
                request.getIsUser(),
                request.getAgentResponse(),
                request.getProcessingTimeMs(),
                request.getErrorMessage(),
                request.getMetadata()
        );

        N8nChatHistoryBase savedMessage = repository.save(message);
        return convertHistoryToMessageDto(savedMessage);
    }

    /**
     * Buscar sesiones por contenido
     */
    @Transactional(readOnly = true)
    public List<SessionDto> searchSessions(String userId, String agentType, String searchTerm) {
        log.debug("Searching sessions for user: {} with term: {}", userId, searchTerm);

        N8nChatHistoryBaseRepository<N8nChatHistoryBase> repository = getRepository(agentType);

        List<N8nChatHistoryBase> messages = repository
                .findMessagesByUserAndContentContaining(userId, searchTerm);

        // Agrupar por sessionId y convertir a SessionDto
        return messages.stream()
                .collect(Collectors.groupingBy(N8nChatHistoryBase::getSessionId))
                .entrySet().stream()
                .map(entry -> {
                    List<N8nChatHistoryBase> sessionMessages = entry.getValue();
                    N8nChatHistoryBase firstMessage = sessionMessages.get(0);

                    return SessionDto.builder()
                            .sessionId(entry.getKey())
                            .userId(userId)
                            .agentType(agentType)
                            .sessionName(generateSessionName(firstMessage.getMessage()))
                            .createdAt(firstMessage.getTimestamp())
                            .updatedAt(sessionMessages.stream()
                                    .map(N8nChatHistoryBase::getTimestamp)
                                    .max(LocalDateTime::compareTo)
                                    .orElse(firstMessage.getTimestamp()))
                            .messageCount(sessionMessages.size())
                            .isActive(true)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Eliminar una sesión y todos sus mensajes
     */
    @Transactional
    public void deleteSession(String sessionId, String userId, String agentType) {
        log.info("Deleting session: {} for user: {} with agent: {}", sessionId, userId, agentType);

        N8nChatHistoryBaseRepository<N8nChatHistoryBase> repository = getRepository(agentType);

        // Verificar que la sesión pertenece al usuario
        if (!repository.existsBySessionIdAndUserId(sessionId, userId)) {
            throw new RuntimeException("Session not found or access denied: " + sessionId);
        }

        // Obtener todos los mensajes de la sesión
        List<N8nChatHistoryBase> messages = repository.findBySessionIdOrderByTimestampAsc(sessionId);

        // Eliminar todos los mensajes
        if (!messages.isEmpty()) {
            repository.deleteAll(messages);
            log.info("Deleted {} messages from session: {}", messages.size(), sessionId);
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

    private SessionDto convertSummaryToSessionDto(Object[] summary, String agentType) {
        String sessionId = (String) summary[0];
        LocalDateTime firstMessage = (LocalDateTime) summary[1];
        LocalDateTime lastMessage = (LocalDateTime) summary[2];
        Long messageCount = (Long) summary[3];

        N8nChatHistoryBaseRepository<N8nChatHistoryBase> repository = getRepository(agentType);

        // Obtener primer mensaje para generar el nombre
        N8nChatHistoryBase firstUserMessage = repository.findFirstUserMessageBySessionId(sessionId);
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

    private MessageDto convertHistoryToMessageDto(N8nChatHistoryBase history) {
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