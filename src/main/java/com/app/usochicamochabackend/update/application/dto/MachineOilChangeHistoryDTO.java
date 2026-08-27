package com.app.usochicamochabackend.update.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "MachineOilChangeHistoryDTO", description = "Registro histórico de cambio de aceite (motor u hidráulico) para una máquina.")
public record MachineOilChangeHistoryDTO(
        Long id,
        LocalDateTime dateStamp,
        String oilType,
        Long brandId,
        String brandName,
        Double quantity,
        Double hourMeter,
        Integer averageHoursChange
) {}
