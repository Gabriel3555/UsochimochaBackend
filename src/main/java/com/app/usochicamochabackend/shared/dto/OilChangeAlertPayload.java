package com.app.usochicamochabackend.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payload unificado para TODAS las alertas (cambio aceite, SOAT, RUNT).
 *
 * Garantiza estructura consistente para:
 * - /topic/alerts/unified (nuevo)
 *
 * Evita múltiples formatos y duplicados en frontend.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OilChangeAlertPayload(
    String id,                      // UUID único para la alerta
    String placa,                   // Identificador del asset (vehículo/moto/máquina)
    String tipoMaquinaria,          // "VEHICULO", "MOTOCICLETA", "MAQUINARIA"
    String tipoAlerta,              // "CAMBIO_ACEITE", "SOAT", "RUNT"
    String descripcion,             // Descripción legible para el operario
    String estado,                  // "ACTIVA", "RESUELTA"
    String colorEstado,             // "ROJO", "AMARILLO", "VERDE"
    Double percentageUsed,          // % de uso (solo para cambio de aceite)
    LocalDate fechaVencimiento,     // Fecha de vencimiento (solo para SOAT/RUNT)
    LocalDateTime timestamp         // Cuándo se generó la alerta
) {
    public static OilChangeAlertPayload fromVehicleMonitoring(
        com.app.usochicamochabackend.vehicle.application.dto.VehicleMonitoringDTO vehicle
    ) {
        if (vehicle.maintenance() == null) return null;

        String descripcion = String.format("🚗 Vehículo (%s) - %s",
            vehicle.placa(),
            vehicle.maintenance().alertMessage());

        return new OilChangeAlertPayload(
            UUID.randomUUID().toString(),
            vehicle.placa(),
            "VEHICULO",
            "CAMBIO_ACEITE",
            descripcion,
            "ACTIVA",
            mapColorToEstado(vehicle.maintenance().alertColor()),
            vehicle.maintenance().percentageUsed(),
            null,
            LocalDateTime.now()
        );
    }

    public static OilChangeAlertPayload fromMotoMonitoring(
        com.app.usochicamochabackend.moto.application.dto.MotoMonitoringDTO moto
    ) {
        if (moto.oil() == null) return null;

        String descripcion = String.format("🏍️ Motocicleta (%s) - %s",
            moto.placa(),
            moto.oil().alertMessage());

        return new OilChangeAlertPayload(
            UUID.randomUUID().toString(),
            moto.placa(),
            "MOTOCICLETA",
            "CAMBIO_ACEITE",
            descripcion,
            "ACTIVA",
            mapColorToEstado(moto.oil().alertColor()),
            moto.oil().percentageUsed(),
            null,
            LocalDateTime.now()
        );
    }

    public static OilChangeAlertPayload fromMachineMonitoring(
        com.app.usochicamochabackend.machine.application.dto.MachineMonitoringDTO machine
    ) {
        var motorInfo = machine.motorOilInfo();
        var hydraulicInfo = machine.hydraulicOilInfo();

        String criticalColor = "GREEN";
        var criticalInfo = motorInfo;

        if (motorInfo != null && isCritical(motorInfo.alertColor(), criticalColor)) {
            criticalColor = motorInfo.alertColor();
            criticalInfo = motorInfo;
        }
        if (hydraulicInfo != null && isCritical(hydraulicInfo.alertColor(), criticalColor)) {
            criticalColor = hydraulicInfo.alertColor();
            criticalInfo = hydraulicInfo;
        }

        if (criticalInfo == null) return null;

        String descripcion = String.format("⚙️ Máquina (%s) - %s [HORAS]",
            machine.machineName(),
            criticalInfo.alertMessage());

        return new OilChangeAlertPayload(
            UUID.randomUUID().toString(),
            String.valueOf(machine.machineId()),
            "MAQUINARIA",
            "CAMBIO_ACEITE",
            descripcion,
            "ACTIVA",
            mapColorToEstado(criticalColor),
            criticalInfo.percentageUsed(),
            null,
            LocalDateTime.now()
        );
    }

    // Factory para alertas SOAT/RUNT con cálculo de color inteligente
    public static OilChangeAlertPayload forSoatAlert(String machineName, Long machineId, LocalDate expirationDate) {
        LocalDate today = LocalDate.now();
        long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, expirationDate);

        // Determinar color basado en días restantes
        // ROJO: 20 días o menos | AMARILLO: 21-45 días | VERDE: más de 45 días
        String colorEstado = "VERDE";
        if (daysUntil <= 0) {
            colorEstado = "ROJO"; // Vencido
        } else if (daysUntil <= 20) {
            colorEstado = "ROJO";
        } else if (daysUntil <= 45) {
            colorEstado = "AMARILLO";
        }

        return new OilChangeAlertPayload(
            UUID.randomUUID().toString(),
            machineName,
            "MAQUINARIA",
            "SOAT",
            String.format("⚠️ El SOAT de %s vence en %d días", machineName, daysUntil),
            "ACTIVA",
            colorEstado,
            null,
            expirationDate,
            LocalDateTime.now()
        );
    }

    public static OilChangeAlertPayload forRuntAlert(String machineName, Long machineId, LocalDate expirationDate) {
        LocalDate today = LocalDate.now();
        long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, expirationDate);

        // Determinar color basado en días restantes
        // ROJO: 20 días o menos | AMARILLO: 21-45 días | VERDE: más de 45 días
        String colorEstado = "VERDE";
        if (daysUntil <= 0) {
            colorEstado = "ROJO"; // Vencido
        } else if (daysUntil <= 20) {
            colorEstado = "ROJO";
        } else if (daysUntil <= 45) {
            colorEstado = "AMARILLO";
        }

        return new OilChangeAlertPayload(
            UUID.randomUUID().toString(),
            machineName,
            "MAQUINARIA",
            "RUNT",
            String.format("⚠️ El RUNT de %s vence en %d días", machineName, daysUntil),
            "ACTIVA",
            colorEstado,
            null,
            expirationDate,
            LocalDateTime.now()
        );
    }

    private static String mapColorToEstado(String alertColor) {
        return switch (alertColor) {
            case "RED" -> "ROJO";
            case "YELLOW" -> "AMARILLO";
            case "GREEN", "BLUE" -> "VERDE";
            default -> "VERDE";
        };
    }

    private static boolean isCritical(String newColor, String currentColor) {
        int newPriority = getColorPriority(newColor);
        int currentPriority = getColorPriority(currentColor);
        return newPriority < currentPriority;
    }

    private static int getColorPriority(String color) {
        return switch (color) {
            case "RED" -> 0;
            case "YELLOW" -> 1;
            case "BLUE" -> 2;
            case "GREEN" -> 3;
            default -> 4;
        };
    }
}
