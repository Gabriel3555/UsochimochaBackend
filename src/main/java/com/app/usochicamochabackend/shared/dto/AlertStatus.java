package com.app.usochicamochabackend.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/**
 * DTO que encapsula el estado de alerta de cambio de aceite.
 * Utilizado por vehículos, motos y máquinas.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AlertStatus(
    String status,                  // "OK", "Programado", "Próximo a Cambio", "Cambio de Aceite"
    String color,                   // "GREEN", "BLUE", "YELLOW", "RED"
    String message,                 // Mensaje para operario
    Double percentageUsed,          // 0-100+
    Long distanceRemaining,         // km o horas hasta próximo cambio (-1 si desconocido)
    LocalDateTime lastChangeDate    // Fecha último cambio (puede ser null)
) {

    public static AlertStatus ok(String message, double percentage, long remaining) {
        return new AlertStatus("OK", "GREEN", message, percentage, remaining, null);
    }

    public static AlertStatus info(String status, String message, double percentage, long remaining) {
        return new AlertStatus(status, "BLUE", message, percentage, remaining, null);
    }

    public static AlertStatus warning(String status, String message, double percentage, long remaining) {
        return new AlertStatus(status, "YELLOW", message, percentage, remaining, null);
    }

    public static AlertStatus critical(String status, String message, double percentage, long remaining) {
        return new AlertStatus(status, "RED", message, percentage, remaining, null);
    }

    public static AlertStatus unknown(String message, double percentage) {
        return new AlertStatus("UNKNOWN", "GRAY", message, percentage, -1L, null);
    }

    public static AlertStatus error(String message, double percentage) {
        return new AlertStatus("ERROR", "RED", message, percentage, -1L, null);
    }

    public AlertStatus withLastChangeDate(LocalDateTime date) {
        return new AlertStatus(this.status, this.color, this.message, this.percentageUsed,
                              this.distanceRemaining, date);
    }
}
