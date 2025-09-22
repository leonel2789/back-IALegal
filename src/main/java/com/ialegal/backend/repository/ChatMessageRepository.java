package com.ialegal.backend.repository;

import com.ialegal.backend.entity.ChatMessage;
import com.ialegal.backend.entity.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Encuentra todos los mensajes de una sesión específica ordenados por timestamp
     */
    List<ChatMessage> findBySession_SessionIdOrderByCreatedAtAsc(String sessionId);

    /**
     * Encuentra mensajes de una sesión con paginación
     */
    Page<ChatMessage> findBySession_SessionIdOrderByCreatedAtAsc(String sessionId, Pageable pageable);

    /**
     * Encuentra mensajes de usuario en una sesión
     */
    List<ChatMessage> findBySession_SessionIdAndIsUserTrueOrderByCreatedAtAsc(String sessionId);

    /**
     * Encuentra mensajes del agente en una sesión
     */
    List<ChatMessage> findBySession_SessionIdAndIsUserFalseOrderByCreatedAtAsc(String sessionId);

    /**
     * Cuenta total de mensajes en una sesión
     */
    long countBySession_SessionId(String sessionId);

    /**
     * Cuenta mensajes de usuario en una sesión
     */
    long countBySession_SessionIdAndIsUserTrue(String sessionId);

    /**
     * Encuentra el último mensaje de una sesión
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.session.sessionId = :sessionId ORDER BY m.createdAt DESC LIMIT 1")
    ChatMessage findLastMessageBySessionId(@Param("sessionId") String sessionId);

    /**
     * Encuentra mensajes recientes en todas las sesiones de un usuario
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.session.userId = :userId AND m.createdAt >= :since ORDER BY m.createdAt DESC")
    List<ChatMessage> findRecentMessagesByUser(@Param("userId") String userId, @Param("since") LocalDateTime since);

    /**
     * Busca mensajes por contenido (para funcionalidad de búsqueda)
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.session.userId = :userId AND " +
           "LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY m.createdAt DESC")
    List<ChatMessage> findMessagesByUserAndContentContaining(
            @Param("userId") String userId,
            @Param("searchTerm") String searchTerm
    );

    /**
     * Encuentra mensajes de un tipo de agente específico para un usuario
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.session.userId = :userId AND m.session.agentType = :agentType ORDER BY m.createdAt DESC")
    List<ChatMessage> findMessagesByUserAndAgentType(
            @Param("userId") String userId,
            @Param("agentType") ChatSession.AgentType agentType
    );

    /**
     * Encuentra mensajes con errores
     */
    List<ChatMessage> findByErrorMessageIsNotNull();

    /**
     * Estadísticas: promedio de tiempo de procesamiento por tipo de agente
     */
    @Query("SELECT m.session.agentType, AVG(m.processingTimeMs) FROM ChatMessage m " +
           "WHERE m.processingTimeMs IS NOT NULL AND m.isUser = false " +
           "GROUP BY m.session.agentType")
    List<Object[]> getAverageProcessingTimeByAgentType();

    /**
     * Encuentra mensajes largos (más de X caracteres)
     */
    @Query("SELECT m FROM ChatMessage m WHERE LENGTH(m.content) > :minLength ORDER BY m.createdAt DESC")
    List<ChatMessage> findLongMessages(@Param("minLength") int minLength);

    /**
     * Elimina mensajes antiguos de sesiones inactivas
     */
    @Query("DELETE FROM ChatMessage m WHERE m.session.isActive = false AND m.createdAt < :cutoffDate")
    int deleteOldMessagesFromInactiveSessions(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Encuentra mensajes sin respuesta del agente (posibles errores)
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.isUser = false AND " +
           "(m.agentResponse IS NULL OR m.agentResponse = '') AND " +
           "m.errorMessage IS NULL")
    List<ChatMessage> findMessagesWithoutAgentResponse();

    /**
     * Obtiene estadísticas de mensajes por usuario
     */
    @Query("SELECT m.session.userId, COUNT(m), AVG(LENGTH(m.content)) FROM ChatMessage m " +
           "WHERE m.isUser = true GROUP BY m.session.userId")
    List<Object[]> getUserMessageStatistics();
}