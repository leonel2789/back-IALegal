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
@CrossOrigin(origins = {"http://localhost:19006", "http://localhost:8081", "exp://192.168.*:19000"})
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
     */
    @GetMapping
    public ResponseEntity<List<SessionDto>> getUserSessions(Authentication authentication) {
        String userId = extractUserIdFromAuth(authentication);
        log.debug("Getting sessions for user: {}", userId);
        List<SessionDto> sessions = sessionService.getUserSessions(userId);
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
        List<SessionDto> sessions = sessionService.getUserSessionsByAgent(userId, agentType);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Obtener sesiones con paginación
     */
    @GetMapping("/agent/{agentType}/paginated")
    public ResponseEntity<Page<SessionDto>> getUserSessionsPaginated(
            @PathVariable String agentType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        String userId = extractUserIdFromAuth(authentication);
        Page<SessionDto> sessions = sessionService.getUserSessionsPaginated(userId, agentType, page, size);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Obtener una sesión específica con sus mensajes
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionDto> getSession(
            @PathVariable String sessionId,
            Authentication authentication) {

        String userId = extractUserIdFromAuth(authentication);
        log.debug("Getting session: {} for user: {}", sessionId, userId);
        SessionDto session = sessionService.getSession(sessionId, userId);
        return ResponseEntity.ok(session);
    }

    /**
     * Obtener mensajes de una sesión
     */
    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<List<MessageDto>> getSessionMessages(
            @PathVariable String sessionId,
            Authentication authentication) {

        String userId = extractUserIdFromAuth(authentication);
        List<MessageDto> messages = sessionService.getSessionMessages(sessionId, userId);
        return ResponseEntity.ok(messages);
    }

    /**
     * Agregar mensaje a una sesión
     */
    @PostMapping("/{sessionId}/messages")
    public ResponseEntity<MessageDto> addMessage(
            @PathVariable String sessionId,
            @Valid @RequestBody AddMessageRequest request,
            Authentication authentication) {

        String userId = extractUserIdFromAuth(authentication);
        log.info("Adding message to session: {} for user: {}", sessionId, userId);
        MessageDto message = sessionService.addMessage(sessionId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    /**
     * Actualizar nombre de sesión
     */
    @PutMapping("/{sessionId}/name")
    public ResponseEntity<SessionDto> updateSessionName(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        String userId = extractUserIdFromAuth(authentication);
        String newName = request.get("sessionName");

        if (newName == null || newName.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        SessionDto session = sessionService.updateSessionName(sessionId, userId, newName);
        return ResponseEntity.ok(session);
    }

    /**
     * Eliminar una sesión
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            @PathVariable String sessionId,
            Authentication authentication) {

        String userId = extractUserIdFromAuth(authentication);
        log.info("Deleting session: {} for user: {}", sessionId, userId);
        sessionService.deleteSession(sessionId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Buscar sesiones por nombre
     */
    @GetMapping("/search")
    public ResponseEntity<List<SessionDto>> searchSessions(
            @RequestParam String query,
            Authentication authentication) {

        String userId = extractUserIdFromAuth(authentication);
        List<SessionDto> sessions = sessionService.searchSessions(userId, query);
        return ResponseEntity.ok(sessions);
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