package com.app.usochicamochabackend.update.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "VehicleOilChangeHistoryDTO", description = "Registro histórico de cambio de aceite para un vehículo o motocicleta.")
public record VehicleOilChangeHistoryDTO(
        Long id,
        LocalDateTime dateStamp,
        // oilType + brandId se agregaron para poder precargar el formulario de
        // edición (brandName por sí solo no alcanza para preseleccionar un <select>).
        String oilType,
        Long brandId,
        String brandName,
        Double quantity,
        Integer kmAtChange,
        Integer intervalKm,
        Boolean airFilterChanged
) {}
