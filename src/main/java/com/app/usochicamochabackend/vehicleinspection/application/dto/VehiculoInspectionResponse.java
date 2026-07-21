package com.app.usochicamochabackend.vehicleinspection.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta al registrar inspección pre-operativa de vehículo.")
public record VehiculoInspectionResponse(
        @Schema(description = "Id numérico creado en `inspeccion_pre_operativa`") Long idInspeccion,
        @Schema(description = "Mensaje de confirmación legible") String mensaje) {
}
