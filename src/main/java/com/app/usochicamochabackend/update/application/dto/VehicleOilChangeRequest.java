package com.app.usochicamochabackend.update.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(
        name = "VehicleOilChangeRequest",
        description = "Registro de cambio de aceite para una placa existente en `vehiculos`. "
                        + "`brandId` referencia la marca de aceite en tabla `brands` (tipo OIL_VEHICLE). "
                        + "`airFilterChanged` alimenta el indicador en monitoreo de motos.")
public record VehicleOilChangeRequest(
        @Schema(description = "Placa del vehículo o moto", example = "ABC123")
        String placa,
        @Schema(description = "Momento del servicio")
        LocalDateTime dateStamp,
        @Schema(description = "Tipo o notación del aceite (texto libre)", example = "15W-40")
        String oilType,
        @Schema(description = "FK a brands.id para aceite de vehículo", example = "2")
        Long brandId,
        @Schema(description = "Cantidad servida (unidad según negocio)", example = "4.5")
        Double quantity,
        @Schema(description = "Odómetro en el momento del cambio", example = "130000")
        Integer kmAtChange,
        @Schema(description = "Intervalo de cambio en km hasta la próxima alerta", example = "10000")
        Integer intervalKm,
        @Schema(description = "Si se reemplazó filtro de aire en el mismo acto", example = "true")
        Boolean airFilterChanged
) {}
