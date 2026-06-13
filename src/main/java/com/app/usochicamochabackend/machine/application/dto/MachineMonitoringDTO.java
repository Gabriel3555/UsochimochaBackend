package com.app.usochicamochabackend.machine.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * DTO consolidado de máquina con estado de aceites (motor + hidráulico).
 */
@Schema(
    name = "MachineMonitoringDTO",
    description = "Fila del tablero de monitoreo de maquinaria: aceites, horas, estado operativo."
)
public record MachineMonitoringDTO(
    @Schema(description = "ID de la máquina") Long machineId,
    @Schema(description = "Nombre descriptivo") String machineName,
    @Schema(description = "Horas de trabajo actuales") Integer horometroActual,
    @Schema(description = "Estado del aceite de motor") OilChangeInfoDTO motorOilInfo,
    @Schema(description = "Estado del aceite hidráulico") OilChangeInfoDTO hydraulicOilInfo,
    @Schema(description = "Último reporte de inspección") LocalDateTime lastInspectionDate
) {
    /**
     * Información de un tipo de aceite específico (motor o hidráulico).
     */
    @Schema(
        name = "MachineOilChangeInfo",
        description = "Detalle del estado de un aceite en máquina."
    )
    public record OilChangeInfoDTO(
        @Schema(description = "Tipo: MOTOR_OIL o HYDRAULIC_OIL") String oilType,
        @Schema(description = "Marca del aceite") String brandName,
        @Schema(description = "Cantidad en litros") Double quantity,
        @Schema(description = "Horas al momento del último cambio") Integer hoursLastChange,
        @Schema(description = "Intervalo de cambio en horas") Integer intervalHours,
        @Schema(description = "Porcentaje de uso del intervalo (0-100+)") Double percentageUsed,
        @Schema(description = "Color de alerta: GREEN, BLUE, YELLOW, RED") String alertColor,
        @Schema(description = "Mensaje de alerta para operario") String alertMessage
    ) {}
}
