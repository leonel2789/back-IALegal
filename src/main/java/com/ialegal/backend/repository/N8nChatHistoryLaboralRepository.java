package com.ialegal.backend.repository;

import com.ialegal.backend.entity.N8nChatHistoryLaboral;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface N8nChatHistoryLaboralRepository extends N8nChatHistoryBaseRepository<N8nChatHistoryLaboral> {

    // Obtener todos los sessionIds distintos de un usuario y agente, ordenados por ID más reciente
    @Query(value = "SELECT DISTINCT session_id " +
            "FROM n8n_chat_histories_laboral " +
            "WHERE session_id LIKE CONCAT(:userId, '_', :agentType, '_%') " +
            "ORDER BY session_id DESC", nativeQuery = true)
    List<String> findDistinctSessionIdsByUserIdAndAgentType(@Param("userId") String userId, @Param("agentType") String agentType);

    // Obtener resumen de sesiones (sessionId, primer mensaje, último mensaje, count)
    @Query(value = "SELECT session_id, " +
            "MIN(id) as firstMessageId, " +
            "MAX(id) as lastMessageId, " +
            "COUNT(*) as messageCount " +
            "FROM n8n_chat_histories_laboral " +
            "WHERE session_id LIKE CONCAT(:userId, '_', :agentType, '_%') " +
            "GROUP BY session_id " +
            "ORDER BY MAX(id) DESC", nativeQuery = true)
    List<Object[]> findSessionSummariesByUserIdAndAgentType(@Param("userId") String userId, @Param("agentType") String agentType);

    // Obtener el primer mensaje de una sesión (el que tiene type = 'human')
    @Query(value = "SELECT * FROM n8n_chat_histories_laboral " +
            "WHERE session_id = :sessionId " +
            "AND message->>'type' = 'human' " +
            "ORDER BY id ASC LIMIT 1", nativeQuery = true)
    N8nChatHistoryLaboral findFirstUserMessageBySessionId(@Param("sessionId") String sessionId);

    // Buscar mensajes por contenido
    @Query(value = "SELECT * FROM n8n_chat_histories_laboral " +
            "WHERE session_id LIKE CONCAT(:userId, '_%') " +
            "AND message->>'content' ILIKE %:searchTerm% " +
            "ORDER BY id DESC", nativeQuery = true)
    List<N8nChatHistoryLaboral> findMessagesByUserAndContentContaining(
            @Param("userId") String userId,
            @Param("searchTerm") String searchTerm
    );

    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END " +
            "FROM n8n_chat_histories_laboral " +
            "WHERE session_id = :sessionId " +
            "AND session_id LIKE CONCAT(:userId, '_%')", nativeQuery = true)
    boolean existsBySessionIdAndUserId(@Param("sessionId") String sessionId, @Param("userId") String userId);
}
