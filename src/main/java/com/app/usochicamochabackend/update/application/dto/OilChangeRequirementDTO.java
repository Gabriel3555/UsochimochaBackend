package com.app.usochicamochabackend.update.application.dto;

import com.app.usochicamochabackend.update.infrastructure.entity.AssetType;
import com.app.usochicamochabackend.update.infrastructure.entity.OilChangeRequirementEntity;
import com.app.usochicamochabackend.update.infrastructure.entity.OilType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO para requisitos de cambio de aceite
 * Expone la configuración de intervalos por tipo de aceite y activo
 */
public record OilChangeRequirementDTO(
    @Schema(description = "ID del requisito", example = "1")
    Long id,

    @Schema(description = "Tipo de aceite", example = "SYNTHETIC")
    String oilType,

    @Schema(description = "Tipo de activo", example = "VEHICLE")
    String assetType,

    @Schema(description = "Rango en km (para vehículos/motos)", example = "10000")
    Integer kmRange,

    @Schema(description = "Rango en horas (para maquinaria)", example = "1000")
    Integer hourRange,

    @Schema(description = "Descripción", example = "100% Sintético - Vehículo - 10000km")
    String description,

    @Schema(description = "¿Está activo?", example = "true")
    Boolean active,

    @Schema(description = "Unidad de medida (km o hrs)", example = "km")
    String unit,

    @Schema(description = "Valor del rango", example = "10000")
    Integer rangeValue,

    @Schema(description = "Durabilidad del aceite", example = "Muy Alta")
    String durabilityLevel
) {
    public static OilChangeRequirementDTO fromEntity(OilChangeRequirementEntity entity) {
        OilType oilType = entity.getOilType();
        return new OilChangeRequirementDTO(
            entity.getId(),
            oilType.name(),
            entity.getAssetType().name(),
            entity.getKmRange(),
            entity.getHourRange(),
            entity.getDescription(),
            entity.getActive(),
            entity.getRangeUnit(),
            entity.getRangeValue(),
            oilType.getDisplayName()
        );
    }
}
