package com.app.usochicamochabackend.update.application.dto;

import com.app.usochicamochabackend.update.infrastructure.entity.VehicleOilChangeEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para cambios de aceite de vehículos
 */
public record VehicleOilChangeResponseDTO(
    @Schema(description = "ID del cambio de aceite", example = "1")
    Long id,

    @Schema(description = "Placa del vehículo", example = "ABC123")
    String placa,

    @Schema(description = "Tipo de aceite", example = "SYNTHETIC")
    String oilType,

    @Schema(description = "Marca de aceite", example = "Mobil")
    String brandName,

    @Schema(description = "Cantidad de litros", example = "5.0")
    Double quantity,

    @Schema(description = "Kilometraje en el cambio", example = "85000")
    Integer kmAtChange,

    @Schema(description = "Próximo cambio recomendado (km)", example = "10000")
    Integer nextChangeKm,

    @Schema(description = "Porcentaje de vida utilizado", example = "45")
    Integer percentageUsed,

    @Schema(description = "¿Se cambió filtro de aire?", example = "true")
    Boolean airFilterChanged,

    @Schema(description = "Fecha del cambio", example = "2026-06-10T14:30:00")
    LocalDateTime dateStamp,

    @Schema(description = "Durabilidad del aceite", example = "Muy Alta")
    String oilDurability
) {
    public static VehicleOilChangeResponseDTO fromEntity(VehicleOilChangeEntity entity) {
        int nextChangeKm = 0;
        String oilDurability = "";
        if (entity.getIntervalKm() != null) {
            nextChangeKm = entity.getKmAtChange() + entity.getIntervalKm();
            oilDurability = entity.getOilType() != null
                ? entity.getOilType().getDurabilityLevel()
                : "No especificado";
        }

        return new VehicleOilChangeResponseDTO(
            entity.getId(),
            entity.getVehicle() != null ? entity.getVehicle().getPlaca() : null,
            entity.getOilType() != null ? entity.getOilType().name() : null,
            entity.getBrand() != null ? entity.getBrand().getName() : null,
            entity.getQuantity(),
            entity.getKmAtChange(),
            nextChangeKm,
            null,
            entity.getAirFilterChanged(),
            entity.getDateStamp(),
            oilDurability
        );
    }
}
