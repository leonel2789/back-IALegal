package com.ialegal.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entidad para historial de chat del agente Laboral.
 * Mapea a la tabla n8n_chat_histories_laboral.
 */
@Entity
@Table(name = "n8n_chat_histories_laboral")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class N8nChatHistoryLaboral extends N8nChatHistoryBase {
    // Esta entidad hereda todos los campos de N8nChatHistoryBase
    // No requiere campos adicionales, solo el mapeo a la tabla específica
}
