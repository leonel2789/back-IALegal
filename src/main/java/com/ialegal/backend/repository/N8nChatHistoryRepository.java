package com.ialegal.backend.repository;

import com.ialegal.backend.entity.N8nChatHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface N8nChatHistoryRepository extends JpaRepository<N8nChatHistory, Long> {

    /**
     * Encuentra mensajes por sessionId ordenados por timestamp
     */
    List<N8nChatHistory> findBySessionIdOrderByTimestampAsc(String sessionId);

    /**
     * Encuentra mensajes por userId ordenados por timestamp
     */
    List<N8nChatHistory> findByUserIdOrderByTimestampDesc(String userId);

    /**
     * Encuentra mensajes por userId y agentType
     */
    List<N8nChatHistory> findByUserIdAndAgentTypeOrderByTimestampDesc(String userId, String agentType);

    /**
     * Encuentra sesiones únicas para un usuario (agrupadas por sessionId)
     */
    @Query("SELECT DISTINCT h.sessionId FROM N8nChatHistory h WHERE h.userId = :userId ORDER BY MAX(h.timestamp) DESC")
    List<String> findDistinctSessionIdsByUserId(@Param("userId") String userId);

    /**
     * Encuentra sesiones únicas para un usuario y agente específico
     */
    @Query("SELECT DISTINCT h.sessionId FROM N8nChatHistory h WHERE h.userId = :userId AND h.agentType = :agentType GROUP BY h.sessionId ORDER BY MAX(h.timestamp) DESC")
    List<String> findDistinctSessionIdsByUserIdAndAgentType(@Param("userId") String userId, @Param("agentType") String agentType);

    /**
     * Obtiene información de sesión (primer y último mensaje)
     */
    @Query("SELECT h.sessionId, MIN(h.timestamp) as firstMessage, MAX(h.timestamp) as lastMessage, COUNT(h) as messageCount " +
           "FROM N8nChatHistory h WHERE h.userId = :userId AND h.agentType = :agentType " +
           "GROUP BY h.sessionId ORDER BY MAX(h.timestamp) DESC")
    List<Object[]> findSessionSummariesByUserIdAndAgentType(@Param("userId") String userId, @Param("agentType") String agentType);

    /**
     * Obtiene el primer mensaje de cada sesión para usar como nombre
     */
    @Query("SELECT h FROM N8nChatHistory h WHERE h.sessionId = :sessionId AND h.isUser = true ORDER BY h.timestamp ASC LIMIT 1")
    N8nChatHistory findFirstUserMessageBySessionId(@Param("sessionId") String sessionId);

    /**
     * Cuenta mensajes por sesión
     */
    long countBySessionId(String sessionId);

    /**
     * Cuenta mensajes de usuario por sesión
     */
    long countBySessionIdAndIsUserTrue(String sessionId);

    /**
     * Busca mensajes por contenido
     */
    @Query("SELECT h FROM N8nChatHistory h WHERE h.userId = :userId AND " +
           "LOWER(h.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY h.timestamp DESC")
    List<N8nChatHistory> findMessagesByUserAndContentContaining(
            @Param("userId") String userId,
            @Param("searchTerm") String searchTerm
    );

    /**
     * Encuentra mensajes recientes
     */
    List<N8nChatHistory> findByUserIdAndTimestampAfterOrderByTimestampDesc(
            String userId,
            LocalDateTime since
    );

    /**
     * Encuentra conversaciones por conversationId
     */
    List<N8nChatHistory> findByConversationIdOrderByTimestampAsc(String conversationId);

    /**
     * Encuentra mensajes con errores
     */
    List<N8nChatHistory> findByErrorMessageIsNotNull();

    /**
     * Estadísticas de uso por usuario
     */
    @Query("SELECT h.userId, COUNT(h), AVG(h.tokensUsed), AVG(h.processingTimeMs) " +
           "FROM N8nChatHistory h WHERE h.tokensUsed IS NOT NULL " +
           "GROUP BY h.userId")
    List<Object[]> getUserUsageStatistics();

    /**
     * Encuentra últimos mensajes por sesión con paginación
     */
    @Query("SELECT h FROM N8nChatHistory h WHERE h.sessionId IN :sessionIds ORDER BY h.timestamp DESC")
    Page<N8nChatHistory> findLastMessagesBySessionIds(@Param("sessionIds") List<String> sessionIds, Pageable pageable);

    /**
     * Elimina mensajes antiguos
     */
    void deleteByTimestampBefore(LocalDateTime cutoffDate);

    /**
     * Verifica si existe una sesión para un usuario
     */
    boolean existsBySessionIdAndUserId(String sessionId, String userId);
}