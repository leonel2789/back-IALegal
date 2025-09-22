package com.ialegal.backend.repository;

import com.ialegal.backend.entity.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

    /**
     * Encuentra todas las sesiones de un usuario específico y tipo de agente
     */
    List<ChatSession> findByUserIdAndAgentTypeOrderByUpdatedAtDesc(
            String userId,
            ChatSession.AgentType agentType
    );

    /**
     * Encuentra todas las sesiones de un usuario específico (todos los agentes)
     */
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(String userId);

    /**
     * Encuentra sesiones activas de un usuario
     */
    List<ChatSession> findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(String userId);

    /**
     * Encuentra sesiones por usuario y agente con paginación
     */
    Page<ChatSession> findByUserIdAndAgentTypeOrderByUpdatedAtDesc(
            String userId,
            ChatSession.AgentType agentType,
            Pageable pageable
    );

    /**
     * Busca sesiones por nombre (para funcionalidad de búsqueda)
     */
    @Query("SELECT s FROM ChatSession s WHERE s.userId = :userId AND " +
           "LOWER(s.sessionName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY s.updatedAt DESC")
    List<ChatSession> findSessionsByUserAndNameContaining(
            @Param("userId") String userId,
            @Param("searchTerm") String searchTerm
    );

    /**
     * Cuenta sesiones activas por usuario
     */
    long countByUserIdAndIsActiveTrue(String userId);

    /**
     * Encuentra sesiones creadas después de una fecha específica
     */
    List<ChatSession> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
            String userId,
            LocalDateTime since
    );

    /**
     * Encuentra la sesión más reciente de un usuario para un agente específico
     */
    Optional<ChatSession> findFirstByUserIdAndAgentTypeOrderByUpdatedAtDesc(
            String userId,
            ChatSession.AgentType agentType
    );

    /**
     * Encuentra sesiones con más de X mensajes
     */
    @Query("SELECT s FROM ChatSession s WHERE s.userId = :userId AND s.messageCount >= :minMessages ORDER BY s.updatedAt DESC")
    List<ChatSession> findSessionsWithMinimumMessages(
            @Param("userId") String userId,
            @Param("minMessages") int minMessages
    );

    /**
     * Actualiza el estado activo de una sesión
     */
    @Modifying
    @Query("UPDATE ChatSession s SET s.isActive = :isActive, s.updatedAt = CURRENT_TIMESTAMP WHERE s.sessionId = :sessionId")
    int updateSessionActiveStatus(@Param("sessionId") String sessionId, @Param("isActive") boolean isActive);

    /**
     * Elimina sesiones inactivas más antiguas que X días
     */
    @Modifying
    @Query("DELETE FROM ChatSession s WHERE s.isActive = false AND s.updatedAt < :cutoffDate")
    int deleteInactiveSessionsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Estadísticas: cuenta sesiones por tipo de agente para un usuario
     */
    @Query("SELECT s.agentType, COUNT(s) FROM ChatSession s WHERE s.userId = :userId GROUP BY s.agentType")
    List<Object[]> countSessionsByAgentType(@Param("userId") String userId);

    /**
     * Encuentra sesiones con actividad reciente (últimos N días)
     */
    @Query("SELECT s FROM ChatSession s WHERE s.userId = :userId AND s.updatedAt >= :since ORDER BY s.updatedAt DESC")
    List<ChatSession> findRecentSessions(@Param("userId") String userId, @Param("since") LocalDateTime since);

    /**
     * Verifica si existe una sesión específica para un usuario
     */
    boolean existsBySessionIdAndUserId(String sessionId, String userId);
}