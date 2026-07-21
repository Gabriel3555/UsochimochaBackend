package com.app.usochicamochabackend.order.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Resumen del vehículo e inspección vinculados a una orden de trabajo.")
public record VehicleOrderSummaryDTO(
        @Schema(description = "PK `inspeccion_pre_operativa`") Long vehicleInspectionId,
        @Schema(description = "Placa del vehículo") String placa,
        @Schema(description = "Marca del vehículo") String marca,
        @Schema(description = "Fecha de la inspección pre-operativa") LocalDateTime fechaInspeccion,
        @Schema(description = "Tipo de vehículo (ej. AUTOMOVIL, MOTO)") String tipoVehiculo,
        @Schema(description = "ID numérico del vehículo") Integer vehiculoId
) {}
