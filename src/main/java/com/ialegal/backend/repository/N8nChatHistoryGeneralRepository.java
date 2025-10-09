package com.ialegal.backend.repository;

import com.ialegal.backend.entity.N8nChatHistoryGeneral;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface N8nChatHistoryGeneralRepository extends N8nChatHistoryBaseRepository<N8nChatHistoryGeneral> {

    @Query(value = "SELECT DISTINCT session_id " +
            "FROM n8n_chat_histories_general " +
            "WHERE session_id LIKE CONCAT(:userId, '_', :agentType, '_%') " +
            "ORDER BY session_id DESC", nativeQuery = true)
    List<String> findDistinctSessionIdsByUserIdAndAgentType(@Param("userId") String userId, @Param("agentType") String agentType);

    @Query(value = "SELECT session_id, " +
            "MIN(id) as firstMessageId, " +
            "MAX(id) as lastMessageId, " +
            "COUNT(*) as messageCount, " +
            "(SELECT created_at FROM n8n_chat_histories_general WHERE session_id = g.session_id ORDER BY id ASC LIMIT 1) as createdAt, " +
            "(SELECT created_at FROM n8n_chat_histories_general WHERE session_id = g.session_id ORDER BY id DESC LIMIT 1) as updatedAt " +
            "FROM n8n_chat_histories_general g " +
            "WHERE session_id LIKE CONCAT(:userId, '_', :agentType, '_%') " +
            "GROUP BY session_id " +
            "ORDER BY MAX(id) DESC", nativeQuery = true)
    List<Object[]> findSessionSummariesByUserIdAndAgentType(@Param("userId") String userId, @Param("agentType") String agentType);

    @Query(value = "SELECT * FROM n8n_chat_histories_general " +
            "WHERE session_id = :sessionId " +
            "AND message->>'type' = 'human' " +
            "ORDER BY id ASC LIMIT 1", nativeQuery = true)
    N8nChatHistoryGeneral findFirstUserMessageBySessionId(@Param("sessionId") String sessionId);

    @Query(value = "SELECT * FROM n8n_chat_histories_general " +
            "WHERE session_id LIKE CONCAT(:userId, '_%') " +
            "AND message->>'content' ILIKE %:searchTerm% " +
            "ORDER BY id DESC", nativeQuery = true)
    List<N8nChatHistoryGeneral> findMessagesByUserAndContentContaining(
            @Param("userId") String userId,
            @Param("searchTerm") String searchTerm
    );

    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END " +
            "FROM n8n_chat_histories_general " +
            "WHERE session_id = :sessionId " +
            "AND session_id LIKE CONCAT(:userId, '_%')", nativeQuery = true)
    boolean existsBySessionIdAndUserId(@Param("sessionId") String sessionId, @Param("userId") String userId);
}
