package com.ialegal.backend.controller;

import com.ialegal.backend.dto.*;
import com.ialegal.backend.service.SessionService;
import com.ialegal.backend.service.N8nSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Slf4j
public class SessionController {

    private final SessionService sessionService;
    private final N8nSessionService n8nSessionService;

    /**
     * Crear nueva sesión
     */
    @PostMapping
    public ResponseEntity<SessionDto> createSession(
            @Valid @RequestBody CreateSessionRequest request,
            Authentication authentication) {

        String userId = extractUserIdFromAuth(authentication);

        log.info("Creating session for user: {} with agent: {}", userId, request.getAgentType());
        // Usar N8nSessionService para trabajar con bases de datos existentes
        SessionDto session = n8nSessionService.createSession(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    /**
     * Obtener todas las sesiones del usuario autenticado
     * NOTA: Este endpoint está deprecado, usar /agent/{agentType} en su lugar
     */
    @GetMapping
    public ResponseEntity<List<SessionDto>> getUserSessions(
            @RequestParam(required = false) String agentType,
            Authentication authentication) {

        String userId = extractUserIdFromAuth(authentication);
        log.debug("Getting sessions for user: {} and agent: {}", userId, agentType);

        // Si no se especifica agentType, usar 'ia-general' por defecto
        String agent = agentType != null ? agentType : "ia-general";
        List<SessionDto> sessions = n8nSessionService.getUserSessionsByAgent(userId, agent);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Obtener sesiones por tipo de agente
     */
    @GetMapping("/agent/{agentType}")
    public ResponseEntity<List<SessionDto>> getUserSessionsByAgent(
            @PathVariable String agentType,
            Authentication authentication) {

        String userId = extractUserIdFromAuth(authentication);
        log.debug("Getting sessions for user: {} and agent: {}", userId, agentType);
        List<SessionDto> sessions = n8nSessionService.getUserSessionsByAgent(userId, agentType);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Obtener una sesión específica con sus mensajes
     * Requiere el agentType como query parameter
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionDto> getSession(
            @PathVariable String sessionId,
            @RequestParam String agentType,
            Authentication authentication) {

        String userId = extractUserIdFromAuth(authentication);
        log.debug("Getting session: {} for user: {} with agent: {}", sessionId, userId, agentType);
        SessionDto session = n8nSessionService.getSession(sessionId, userId, agentType);
        return ResponseEntity.ok(session);
    }

    /**
     * Obtener mensajes de una sesión
     * Requiere el agentType como query parameter
     */
    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<List<MessageDto>> getSessionMessages(
            @PathVariable String sessionId,
            @RequestParam String agentType,
            Authentication authentication) {

        String userId = extractUserIdFromAuth(authentication);
        log.debug("Getting messages for session: {} with agent: {}", sessionId, agentType);
        List<MessageDto> messages = n8nSessionService.getSessionMessages(sessionId, userId, agentType);
        return ResponseEntity.ok(messages);
    }

    /**
     * Agregar mensaje a una sesión
     * Requiere el agentType como query parameter
     */
    @PostMapping("/{sessionId}/messages")
    public ResponseEntity<MessageDto> addMessage(
            @PathVariable String sessionId,
            @RequestParam String agentType,
            @Valid @RequestBody AddMessageRequest request,
            Authentication authentication) {

        String userId = extractUserIdFromAuth(authentication);
        log.info("Adding message to session: {} for user: {} with agent: {}", sessionId, userId, agentType);
        MessageDto message = n8nSessionService.addMessage(sessionId, userId, agentType, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    /**
     * Buscar sesiones por contenido
     * Requiere el agentType como query parameter
     */
    @GetMapping("/search")
    public ResponseEntity<List<SessionDto>> searchSessions(
            @RequestParam String query,
            @RequestParam String agentType,
            Authentication authentication) {

        String userId = extractUserIdFromAuth(authentication);
        log.debug("Searching sessions for user: {} with query: {} and agent: {}", userId, query, agentType);
        List<SessionDto> sessions = n8nSessionService.searchSessions(userId, agentType, query);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Eliminar una sesión
     * Requiere el agentType como query parameter
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            @PathVariable String sessionId,
            @RequestParam String agentType,
            Authentication authentication) {

        String userId = extractUserIdFromAuth(authentication);
        log.info("Deleting session: {} for user: {} with agent: {}", sessionId, userId, agentType);
        n8nSessionService.deleteSession(sessionId, userId, agentType);
        return ResponseEntity.noContent().build();
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", System.currentTimeMillis(),
                "service", "session-management"
        ));
    }

    // Métodos de utilidad

    /**
     * Extraer userId del token JWT
     */
    private String extractUserIdFromAuth(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        if (authentication.getPrincipal() instanceof Jwt jwt) {
            // Intentar obtener el userId de diferentes claims del JWT
            String userId = jwt.getClaimAsString("preferred_username");
            if (userId == null) {
                userId = jwt.getClaimAsString("sub");
            }
            if (userId == null) {
                userId = jwt.getClaimAsString("username");
            }

            if (userId == null) {
                throw new RuntimeException("User ID not found in JWT token");
            }

            return userId;
        }

        throw new RuntimeException("Invalid authentication principal");
    }

    /**
     * Manejo de errores
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime error: ", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", ex.getMessage(),
                        "timestamp", System.currentTimeMillis()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Validation error: ", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "Invalid request: " + ex.getMessage(),
                        "timestamp", System.currentTimeMillis()
                ));
    }
}