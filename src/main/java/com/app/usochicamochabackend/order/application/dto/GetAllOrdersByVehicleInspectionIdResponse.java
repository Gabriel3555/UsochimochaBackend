package com.app.usochicamochabackend.order.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Lista de órdenes de trabajo agrupadas por inspección pre-operativa de vehículo.")
public record GetAllOrdersByVehicleInspectionIdResponse(
        @Schema(description = "PK `inspeccion_pre_operativa`") Long vehicleInspectionId,
        @Schema(description = "Placa del vehículo inspeccionado") String placa,
        @Schema(description = "Fecha de la inspección") LocalDateTime fechaInspeccion,
        @Schema(description = "Órdenes asignadas a esta inspección") List<OrderWithoutInspectionResponse> orders
) {}
