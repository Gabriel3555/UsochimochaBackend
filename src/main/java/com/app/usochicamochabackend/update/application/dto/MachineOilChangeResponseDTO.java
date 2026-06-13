package com.app.usochicamochabackend.update.application.dto;

import com.app.usochicamochabackend.update.infrastructure.entity.OilChangeEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para cambios de aceite en maquinaria
 */
public record MachineOilChangeResponseDTO(
    @Schema(description = "ID del cambio de aceite", example = "1")
    Long id,

    @Schema(description = "ID de la máquina", example = "10")
    Long machineId,

    @Schema(description = "Tipo de aceite", example = "SYNTHETIC")
    String oilType,

    @Schema(description = "Marca de aceite", example = "Shell")
    String brandName,

    @Schema(description = "Cantidad de litros", example = "10.0")
    Double quantity,

    @Schema(description = "Horometro en el cambio", example = "750")
    Integer hourStamp,

    @Schema(description = "Próximo cambio recomendado (horas)", example = "1000")
    Integer nextChangeHours,

    @Schema(description = "Porcentaje de vida utilizado", example = "75")
    Integer percentageUsed,

    @Schema(description = "¿Es aceite motor?", example = "true")
    Boolean motorOil,

    @Schema(description = "¿Es aceite hidráulico?", example = "false")
    Boolean hydraulicOil,

    @Schema(description = "Fecha del cambio", example = "2026-06-10T14:30:00")
    LocalDateTime dateStamp,

    @Schema(description = "Durabilidad del aceite", example = "Muy Alta")
    String oilDurability
) {
    public static MachineOilChangeResponseDTO fromEntity(OilChangeEntity entity) {
        int nextChangeHours = 0;
        String oilDurability = "";
        if (entity.getRequirement() != null) {
            nextChangeHours = entity.getHourStamp() + entity.getRequirement().getHourRange();
            oilDurability = entity.getOilType() != null
                ? entity.getOilType().getDisplayName()
                : "No especificado";
        }

        return new MachineOilChangeResponseDTO(
            entity.getId(),
            entity.getMachine() != null ? entity.getMachine().getId() : null,
            entity.getOilType() != null ? entity.getOilType().name() : null,
            entity.getBrand() != null ? entity.getBrand().getName() : null,
            entity.getQuantity(),
            entity.getHourStamp(),
            nextChangeHours,
            entity.getPercentageUsed(),
            entity.getMotorOil(),
            entity.getHydraulicOil(),
            entity.getDateStamp(),
            oilDurability
        );
    }
}
