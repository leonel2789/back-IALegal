package com.ialegal.backend.repository;

import com.ialegal.backend.entity.N8nChatHistoryBase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Interfaz base para todos los repositorios de historial de chat N8N.
 * Define los métodos comunes que todos los repositorios específicos deben implementar.
 */
@NoRepositoryBean
public interface N8nChatHistoryBaseRepository<T extends N8nChatHistoryBase> extends JpaRepository<T, Long> {

    List<T> findBySessionIdOrderByTimestampAsc(String sessionId);

    List<T> findByUserIdOrderByTimestampDesc(String userId);

    List<T> findByUserIdAndAgentTypeOrderByTimestampDesc(String userId, String agentType);

    List<String> findDistinctSessionIdsByUserId(@Param("userId") String userId);

    List<String> findDistinctSessionIdsByUserIdAndAgentType(@Param("userId") String userId, @Param("agentType") String agentType);

    List<Object[]> findSessionSummariesByUserIdAndAgentType(@Param("userId") String userId, @Param("agentType") String agentType);

    T findFirstUserMessageBySessionId(@Param("sessionId") String sessionId);

    long countBySessionId(String sessionId);

    long countBySessionIdAndIsUserTrue(String sessionId);

    List<T> findMessagesByUserAndContentContaining(@Param("userId") String userId, @Param("searchTerm") String searchTerm);

    List<T> findByUserIdAndTimestampAfterOrderByTimestampDesc(String userId, LocalDateTime since);

    List<T> findByConversationIdOrderByTimestampAsc(String conversationId);

    List<T> findByErrorMessageIsNotNull();

    List<Object[]> getUserUsageStatistics();

    Page<T> findLastMessagesBySessionIds(@Param("sessionIds") List<String> sessionIds, Pageable pageable);

    void deleteByTimestampBefore(LocalDateTime cutoffDate);

    boolean existsBySessionIdAndUserId(String sessionId, String userId);
}
