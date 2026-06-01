package com.app.usochicamochabackend.order.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Solicitud para crear una orden de trabajo sobre una inspección pre-operativa de vehículo.")
public record AssignVehicleOrderRequest(
        @Schema(description = "PK de `inspeccion_pre_operativa`") Long vehicleInspectionId,
        @Schema(description = "Descripción / observación de la orden") String description,
        @Schema(description = "Tipo de orden") String orderType,
        @Schema(description = "Tipo de mantenimiento: ELECTRICO, MECANICO, ESTRUCTURAL") String maintenanceType
) {}
