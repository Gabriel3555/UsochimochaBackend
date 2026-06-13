package com.app.usochicamochabackend.update.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "VehicleOilChangeHistoryDTO", description = "Registro histórico de cambio de aceite para un vehículo o motocicleta.")
public record VehicleOilChangeHistoryDTO(
        Long id,
        LocalDateTime dateStamp,
        String brandName,
        Double quantity,
        Integer kmAtChange,
        Integer intervalKm,
        Boolean airFilterChanged
) {}
