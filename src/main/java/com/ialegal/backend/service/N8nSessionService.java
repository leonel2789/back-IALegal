package com.ialegal.backend.service;

import com.ialegal.backend.dto.*;
import com.ialegal.backend.entity.*;
import com.ialegal.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
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
     * Crear nueva sesión (solo genera sessionId, N8N crea los mensajes)
     */
    @Transactional(readOnly = true)
    public SessionDto createSession(String userId, CreateSessionRequest request) {
        log.info("Generating new sessionId for user: {} with agent: {}", userId, request.getAgentType());

        String agentType = request.getAgentType();

        // Generar sessionId único (formato: userId_agentType_timestamp_uuid)
        String sessionId = generateSessionId(userId, agentType);

        // Generar nombre de sesión
        String sessionName = request.getSessionName();
        if (sessionName == null || sessionName.trim().isEmpty()) {
            sessionName = request.getFirstMessage() != null ?
                generateSessionName(request.getFirstMessage()) : "Nueva conversación";
        }

        // NOTA: NO guardamos nada en DB. Los mensajes los crea N8N cuando procesa el webhook.
        // Solo retornamos el sessionId generado para que el frontend lo use.

        return SessionDto.builder()
                .sessionId(sessionId)
                .userId(userId)
                .agentType(agentType)
                .sessionName(sessionName)
                .createdAt(null)
                .updatedAt(null)
                .messageCount(0) // Aún no hay mensajes, los creará N8N
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

        // Obtener mensajes de la sesión ordenados por ID
        List<N8nChatHistoryBase> messages = repository.findBySessionIdOrderByIdAsc(sessionId);

        if (messages.isEmpty()) {
            throw new RuntimeException("Session not found: " + sessionId);
        }

        // Convertir a DTOs
        List<MessageDto> messageDtos = messages.stream()
                .map(this::convertHistoryToMessageDto)
                .collect(Collectors.toList());

        // Crear SessionDto
        N8nChatHistoryBase firstUserMsg = messages.stream()
                .filter(N8nChatHistoryBase::isUserMessage)
                .findFirst()
                .orElse(messages.get(0));

        String sessionName = generateSessionName(firstUserMsg.getContent());

        return SessionDto.builder()
                .sessionId(sessionId)
                .userId(userId)
                .agentType(agentType)
                .sessionName(sessionName)
                .createdAt(null) // No tenemos timestamp real
                .updatedAt(null)
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

        // Verificar si la sesión existe
        // Si no existe, retornar array vacío (puede ser una sesión nueva sin mensajes aún)
        if (!repository.existsBySessionIdAndUserId(sessionId, userId)) {
            log.debug("Session {} not found in DB yet - returning empty array (new session)", sessionId);
            return Collections.emptyList();
        }

        List<N8nChatHistoryBase> messages = repository.findBySessionIdOrderByIdAsc(sessionId);
        return messages.stream()
                .map(this::convertHistoryToMessageDto)
                .collect(Collectors.toList());
    }

    /**
     * Agregar mensaje a sesión
     * DEPRECATED: Los mensajes los debe crear N8N, no el backend.
     * Este método solo existe para compatibilidad con el frontend.
     */
    @Deprecated
    @Transactional(readOnly = true)
    public MessageDto addMessage(String sessionId, String userId, String agentType, AddMessageRequest request) {
        log.warn("addMessage called but backend should NOT create messages. N8N creates them.");

        // Retornar un DTO vacío sin hacer nada
        // El frontend debe enviar el mensaje al webhook N8N, no al backend
        return MessageDto.builder()
                .sessionId(sessionId)
                .content(request.getContent())
                .isUser(request.getIsUser())
                .build();
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
                    N8nChatHistoryBase firstUserMsg = sessionMessages.stream()
                            .filter(N8nChatHistoryBase::isUserMessage)
                            .findFirst()
                            .orElse(sessionMessages.get(0));

                    return SessionDto.builder()
                            .sessionId(entry.getKey())
                            .userId(userId)
                            .agentType(agentType)
                            .sessionName(generateSessionName(firstUserMsg.getContent()))
                            .createdAt(null) // No tenemos timestamp
                            .updatedAt(null)
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

        // Eliminar todos los mensajes usando el método del repositorio
        repository.deleteBySessionId(sessionId);
        log.info("Deleted all messages from session: {}", sessionId);
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
        // summary[1] y [2] son IDs ahora, no timestamps
        Long messageCount = ((Number) summary[3]).longValue();

        N8nChatHistoryBaseRepository<N8nChatHistoryBase> repository = getRepository(agentType);

        // Obtener primer mensaje de usuario para generar el nombre
        N8nChatHistoryBase firstUserMessage = repository.findFirstUserMessageBySessionId(sessionId);
        String sessionName = firstUserMessage != null ?
                generateSessionName(firstUserMessage.getContent()) : "Conversación";

        // Extraer userId del sessionId
        String userId = sessionId.split("_")[0];

        return SessionDto.builder()
                .sessionId(sessionId)
                .userId(userId)
                .agentType(agentType)
                .sessionName(sessionName)
                .createdAt(null) // No tenemos timestamp real
                .updatedAt(null)
                .messageCount(messageCount.intValue())
                .isActive(true)
                .build();
    }

    private MessageDto convertHistoryToMessageDto(N8nChatHistoryBase history) {
        return MessageDto.builder()
                .id(history.getId())
                .sessionId(history.getSessionId())
                .content(history.getContent()) // Parsea el JSONB y extrae content
                .isUser(history.isUserMessage()) // Verifica type = 'human'
                .createdAt(null) // No tenemos timestamp
                .agentResponse(history.isAiMessage() ? history.getContent() : null)
                .processingTimeMs(null)
                .errorMessage(null)
                .metadata(null)
                .build();
    }
}