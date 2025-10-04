package com.ialegal.backend.repository;

import com.ialegal.backend.entity.N8nChatHistoryLaboral;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface N8nChatHistoryLaboralRepository extends N8nChatHistoryBaseRepository<N8nChatHistoryLaboral> {

    List<N8nChatHistoryLaboral> findBySessionIdOrderByTimestampAsc(String sessionId);

    List<N8nChatHistoryLaboral> findByUserIdOrderByTimestampDesc(String userId);

    List<N8nChatHistoryLaboral> findByUserIdAndAgentTypeOrderByTimestampDesc(String userId, String agentType);

    @Query("SELECT DISTINCT h.sessionId FROM N8nChatHistoryLaboral h WHERE h.userId = :userId ORDER BY MAX(h.timestamp) DESC")
    List<String> findDistinctSessionIdsByUserId(@Param("userId") String userId);

    @Query("SELECT DISTINCT h.sessionId FROM N8nChatHistoryLaboral h WHERE h.userId = :userId AND h.agentType = :agentType GROUP BY h.sessionId ORDER BY MAX(h.timestamp) DESC")
    List<String> findDistinctSessionIdsByUserIdAndAgentType(@Param("userId") String userId, @Param("agentType") String agentType);

    @Query("SELECT h.sessionId, MIN(h.timestamp) as firstMessage, MAX(h.timestamp) as lastMessage, COUNT(h) as messageCount " +
           "FROM N8nChatHistoryLaboral h WHERE h.userId = :userId AND h.agentType = :agentType " +
           "GROUP BY h.sessionId ORDER BY MAX(h.timestamp) DESC")
    List<Object[]> findSessionSummariesByUserIdAndAgentType(@Param("userId") String userId, @Param("agentType") String agentType);

    @Query("SELECT h FROM N8nChatHistoryLaboral h WHERE h.sessionId = :sessionId AND h.isUser = true ORDER BY h.timestamp ASC LIMIT 1")
    N8nChatHistoryLaboral findFirstUserMessageBySessionId(@Param("sessionId") String sessionId);

    long countBySessionId(String sessionId);

    long countBySessionIdAndIsUserTrue(String sessionId);

    @Query("SELECT h FROM N8nChatHistoryLaboral h WHERE h.userId = :userId AND " +
           "LOWER(h.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY h.timestamp DESC")
    List<N8nChatHistoryLaboral> findMessagesByUserAndContentContaining(
            @Param("userId") String userId,
            @Param("searchTerm") String searchTerm
    );

    List<N8nChatHistoryLaboral> findByUserIdAndTimestampAfterOrderByTimestampDesc(
            String userId,
            LocalDateTime since
    );

    List<N8nChatHistoryLaboral> findByConversationIdOrderByTimestampAsc(String conversationId);

    List<N8nChatHistoryLaboral> findByErrorMessageIsNotNull();

    @Query("SELECT h.userId, COUNT(h), AVG(h.tokensUsed), AVG(h.processingTimeMs) " +
           "FROM N8nChatHistoryLaboral h WHERE h.tokensUsed IS NOT NULL " +
           "GROUP BY h.userId")
    List<Object[]> getUserUsageStatistics();

    @Query("SELECT h FROM N8nChatHistoryLaboral h WHERE h.sessionId IN :sessionIds ORDER BY h.timestamp DESC")
    Page<N8nChatHistoryLaboral> findLastMessagesBySessionIds(@Param("sessionIds") List<String> sessionIds, Pageable pageable);

    void deleteByTimestampBefore(LocalDateTime cutoffDate);

    boolean existsBySessionIdAndUserId(String sessionId, String userId);
}
