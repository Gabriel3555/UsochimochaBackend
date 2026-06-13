package com.app.usochicamochabackend.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/**
 * Payload que se envía por WebSocket cuando hay una alerta de cambio de aceite.
 *
 * Usado en: /topic/oil-change-alerts
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OilChangeAlertPayload(
    String placa,                   // Identificador del vehículo/moto
    String tipoMaquinaria,          // "VEHICULO", "MOTOCICLETA"
    String alertColor,              // "GREEN", "BLUE", "YELLOW", "RED"
    String alertStatus,             // "OK", "Programado", "Próximo a Cambio", "Cambio de Aceite"
    String alertMessage,            // Mensaje para operario
    Double percentageUsed,          // % de uso
    LocalDateTime timestamp         // Cuándo ocurrió la alerta
) {
    public static OilChangeAlertPayload fromVehicleMonitoring(
        com.app.usochicamochabackend.vehicle.application.dto.VehicleMonitoringDTO vehicle
    ) {
        if (vehicle.maintenance() == null) return null;

        return new OilChangeAlertPayload(
            vehicle.placa(),
            "VEHICULO",
            vehicle.maintenance().alertColor(),
            vehicle.maintenance().estado(),
            vehicle.maintenance().alertMessage(),
            vehicle.maintenance().percentageUsed(),
            LocalDateTime.now()
        );
    }

    public static OilChangeAlertPayload fromMotoMonitoring(
        com.app.usochicamochabackend.moto.application.dto.MotoMonitoringDTO moto
    ) {
        if (moto.oil() == null) return null;

        return new OilChangeAlertPayload(
            moto.placa(),
            "MOTOCICLETA",
            moto.oil().alertColor(),
            moto.oil().estado(),
            moto.oil().alertMessage(),
            moto.oil().percentageUsed(),
            LocalDateTime.now()
        );
    }
}
