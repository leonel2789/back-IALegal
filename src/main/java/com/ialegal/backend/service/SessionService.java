package com.ialegal.backend.service;

import com.ialegal.backend.dto.*;
import com.ialegal.backend.entity.ChatMessage;
import com.ialegal.backend.entity.ChatSession;
import com.ialegal.backend.repository.ChatMessageRepository;
import com.ialegal.backend.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SessionService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    /**
     * Crear una nueva sesión de chat
     */
    public SessionDto createSession(CreateSessionRequest request) {
        log.info("Creating new session for user: {} with agent: {}", request.getUserId(), request.getAgentType());

        // Generar ID único para la sesión
        String sessionId = generateSessionId(request.getUserId(), request.getAgentType());

        // Validar y convertir tipo de agente
        ChatSession.AgentType agentType = ChatSession.AgentType.fromValue(request.getAgentType());

        // Generar nombre de sesión si no se proporciona
        String sessionName = request.getSessionName();
        if (sessionName == null || sessionName.trim().isEmpty()) {
            if (request.getFirstMessage() != null && !request.getFirstMessage().trim().isEmpty()) {
                sessionName = generateSessionName(request.getFirstMessage());
            } else {
                sessionName = "Nueva conversación";
            }
        }

        // Crear sesión
        ChatSession session = ChatSession.builder()
                .sessionId(sessionId)
                .userId(request.getUserId())
                .agentType(agentType)
                .sessionName(sessionName.trim())
                .isActive(true)
                .messageCount(0)
                .build();

        // Agregar primer mensaje si se proporciona
        if (request.getFirstMessage() != null && !request.getFirstMessage().trim().isEmpty()) {
            ChatMessage firstMessage = ChatMessage.userMessage(session, request.getFirstMessage().trim());
            session.addMessage(firstMessage);
        }

        ChatSession savedSession = sessionRepository.save(session);
        log.info("Created session: {} with {} messages", sessionId, savedSession.getMessageCount());

        return SessionDto.fromEntity(savedSession);
    }

    /**
     * Obtener todas las sesiones de un usuario
     */
    @Transactional(readOnly = true)
    public List<SessionDto> getUserSessions(String userId) {
        log.debug("Getting sessions for user: {}", userId);
        List<ChatSession> sessions = sessionRepository.findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId);
        return sessions.stream()
                .map(SessionDto::fromEntity)
                .toList();
    }

    /**
     * Obtener sesiones de un usuario por tipo de agente
     */
    @Transactional(readOnly = true)
    public List<SessionDto> getUserSessionsByAgent(String userId, String agentType) {
        log.debug("Getting sessions for user: {} and agent: {}", userId, agentType);
        ChatSession.AgentType agent = ChatSession.AgentType.fromValue(agentType);
        List<ChatSession> sessions = sessionRepository.findByUserIdAndAgentTypeOrderByUpdatedAtDesc(userId, agent);
        return sessions.stream()
                .map(SessionDto::fromEntity)
                .toList();
    }

    /**
     * Obtener sesiones con paginación
     */
    @Transactional(readOnly = true)
    public Page<SessionDto> getUserSessionsPaginated(String userId, String agentType, int page, int size) {
        ChatSession.AgentType agent = ChatSession.AgentType.fromValue(agentType);
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatSession> sessionPage = sessionRepository.findByUserIdAndAgentTypeOrderByUpdatedAtDesc(userId, agent, pageable);
        return sessionPage.map(SessionDto::fromEntity);
    }

    /**
     * Obtener una sesión específica con sus mensajes
     */
    @Transactional(readOnly = true)
    public SessionDto getSession(String sessionId, String userId) {
        log.debug("Getting session: {} for user: {}", sessionId, userId);

        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        // Verificar que la sesión pertenece al usuario
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied to session: " + sessionId);
        }

        return SessionDto.fromEntityWithMessages(session);
    }

    /**
     * Agregar mensaje a una sesión
     */
    public MessageDto addMessage(String sessionId, String userId, AddMessageRequest request) {
        log.debug("Adding message to session: {} for user: {}", sessionId, userId);

        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        // Verificar que la sesión pertenece al usuario
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied to session: " + sessionId);
        }

        // Crear mensaje
        ChatMessage message = ChatMessage.builder()
                .session(session)
                .content(request.getContent())
                .isUser(request.getIsUser())
                .agentResponse(request.getAgentResponse())
                .processingTimeMs(request.getProcessingTimeMs())
                .errorMessage(request.getErrorMessage())
                .metadata(request.getMetadata())
                .build();

        // Actualizar sesión
        session.addMessage(message);
        session.setUpdatedAt(LocalDateTime.now());

        ChatMessage savedMessage = messageRepository.save(message);
        sessionRepository.save(session);

        log.info("Added message to session: {}, total messages: {}", sessionId, session.getMessageCount());
        return MessageDto.fromEntity(savedMessage);
    }

    /**
     * Obtener mensajes de una sesión
     */
    @Transactional(readOnly = true)
    public List<MessageDto> getSessionMessages(String sessionId, String userId) {
        log.debug("Getting messages for session: {} and user: {}", sessionId, userId);

        // Verificar que la sesión existe y pertenece al usuario
        if (!sessionRepository.existsBySessionIdAndUserId(sessionId, userId)) {
            throw new RuntimeException("Session not found or access denied: " + sessionId);
        }

        List<ChatMessage> messages = messageRepository.findBySession_SessionIdOrderByCreatedAtAsc(sessionId);
        return messages.stream()
                .map(MessageDto::fromEntity)
                .toList();
    }

    /**
     * Eliminar una sesión
     */
    public void deleteSession(String sessionId, String userId) {
        log.info("Deleting session: {} for user: {}", sessionId, userId);

        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        // Verificar que la sesión pertenece al usuario
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied to session: " + sessionId);
        }

        sessionRepository.delete(session);
        log.info("Deleted session: {}", sessionId);
    }

    /**
     * Actualizar nombre de sesión
     */
    public SessionDto updateSessionName(String sessionId, String userId, String newName) {
        log.info("Updating session name: {} for user: {}", sessionId, userId);

        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        // Verificar que la sesión pertenece al usuario
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied to session: " + sessionId);
        }

        session.setSessionName(newName.trim());
        session.setUpdatedAt(LocalDateTime.now());

        ChatSession updatedSession = sessionRepository.save(session);
        return SessionDto.fromEntity(updatedSession);
    }

    /**
     * Buscar sesiones por nombre
     */
    @Transactional(readOnly = true)
    public List<SessionDto> searchSessions(String userId, String searchTerm) {
        log.debug("Searching sessions for user: {} with term: {}", userId, searchTerm);
        List<ChatSession> sessions = sessionRepository.findSessionsByUserAndNameContaining(userId, searchTerm);
        return sessions.stream()
                .map(SessionDto::fromEntity)
                .toList();
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
}