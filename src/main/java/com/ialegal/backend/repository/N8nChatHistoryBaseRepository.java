package com.ialegal.backend.repository;

import com.ialegal.backend.entity.N8nChatHistoryBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Interfaz base para todos los repositorios de historial de chat N8N.
 * Adaptado a la estructura real de N8N: id, session_id, message (JSONB)
 */
@NoRepositoryBean
public interface N8nChatHistoryBaseRepository<T extends N8nChatHistoryBase> extends JpaRepository<T, Long> {

    // Obtener todos los mensajes de una sesión, ordenados por ID
    List<T> findBySessionIdOrderByIdAsc(String sessionId);

    // Contar mensajes en una sesión
    long countBySessionId(String sessionId);

    // Verificar si una sesión pertenece a un usuario (extrayendo userId del sessionId)
    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END " +
            "FROM #{#entityName} " +
            "WHERE session_id = :sessionId " +
            "AND session_id LIKE CONCAT(:userId, '_%')", nativeQuery = true)
    boolean existsBySessionIdAndUserId(@Param("sessionId") String sessionId, @Param("userId") String userId);

    // Eliminar todos los mensajes de una sesión
    void deleteBySessionId(String sessionId);

    // Métodos que deben ser implementados por cada repositorio específico
    List<Object[]> findSessionSummariesByUserIdAndAgentType(@Param("userId") String userId, @Param("agentType") String agentType);

    T findFirstUserMessageBySessionId(@Param("sessionId") String sessionId);

    List<T> findMessagesByUserAndContentContaining(@Param("userId") String userId, @Param("searchTerm") String searchTerm);
}
