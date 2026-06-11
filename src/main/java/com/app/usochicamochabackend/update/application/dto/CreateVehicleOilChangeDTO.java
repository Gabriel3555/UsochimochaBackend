package com.app.usochicamochabackend.update.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * DTO para crear un cambio de aceite en vehículo o motocicleta
 */
public record CreateVehicleOilChangeDTO(
    @NotNull(message = "Placa es requerida")
    @Schema(description = "Placa del vehículo", example = "ABC123")
    String placa,

    @NotNull(message = "Tipo de aceite es requerido")
    @Schema(description = "Tipo de aceite (MINERAL, SEMI_SYNTHETIC, SYNTHETIC)", example = "SYNTHETIC")
    String oilType,

    @NotNull(message = "ID de marca es requerido")
    @Schema(description = "ID de marca de aceite", example = "1")
    Long brandId,

    @NotNull(message = "Cantidad es requerida")
    @Positive(message = "Cantidad debe ser positiva")
    @Schema(description = "Cantidad de litros", example = "5.0")
    Double quantity,

    @NotNull(message = "Kilometraje es requerido")
    @PositiveOrZero(message = "Kilometraje no puede ser negativo")
    @Schema(description = "Kilometraje en el cambio", example = "85000")
    Integer kmAtChange,

    @Schema(description = "¿Se cambió filtro de aire?", example = "true")
    Boolean airFilterChanged,

    @NotNull(message = "ID de requisito es requerido")
    @Schema(description = "ID del requisito de cambio (define el próximo intervalo)", example = "15")
    Long requirementId
) {
}
