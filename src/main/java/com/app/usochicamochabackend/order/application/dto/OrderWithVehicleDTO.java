package com.app.usochicamochabackend.order.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Orden de trabajo vinculada a una inspección pre-operativa de vehículo.")
public record OrderWithVehicleDTO(
        OrderWithoutInspectionResponse order,
        VehicleOrderSummaryDTO vehicle
) {}
