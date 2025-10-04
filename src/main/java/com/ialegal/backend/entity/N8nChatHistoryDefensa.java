package com.ialegal.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * Entidad para historial de chat del agente de Defensa del Consumidor.
 * Mapea a la tabla n8n_chat_histories_defensa.
 */
@Entity
@Table(name = "n8n_chat_histories_defensa")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class N8nChatHistoryDefensa extends N8nChatHistoryBase {
    // Esta entidad hereda todos los campos de N8nChatHistoryBase
    // No requiere campos adicionales, solo el mapeo a la tabla específica

    public N8nChatHistoryDefensa() {
        super();
    }
}
