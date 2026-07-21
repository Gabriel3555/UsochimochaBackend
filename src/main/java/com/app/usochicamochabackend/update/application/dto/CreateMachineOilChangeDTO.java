package com.app.usochicamochabackend.update.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * DTO para crear cambios de aceite en maquinaria
 */
public record CreateMachineOilChangeDTO(
    @NotNull(message = "ID de máquina es requerido")
    @Schema(description = "ID de la máquina", example = "10")
    Long machineId,

    @NotNull(message = "Tipo de aceite es requerido")
    @Schema(description = "Tipo de aceite (MINERAL, SEMI_SYNTHETIC, SYNTHETIC)", example = "SYNTHETIC")
    String oilType,

    @Schema(description = "¿Es cambio de aceite motor?", example = "true")
    Boolean motorOil,

    @Schema(description = "¿Es cambio de aceite hidráulico?", example = "false")
    Boolean hydraulicOil,

    @NotNull(message = "ID de marca es requerido")
    @Schema(description = "ID de marca de aceite", example = "1")
    Long brandId,

    @NotNull(message = "Cantidad es requerida")
    @Positive(message = "Cantidad debe ser positiva")
    @Schema(description = "Cantidad de litros", example = "10.0")
    Double quantity,

    @NotNull(message = "Horometro es requerido")
    @PositiveOrZero(message = "Horometro no puede ser negativo")
    @Schema(description = "Horas en el cambio", example = "750")
    Integer hourStamp,

    @NotNull(message = "ID de requisito es requerido")
    @Schema(description = "ID del requisito de cambio", example = "21")
    Long requirementId
) {
}
