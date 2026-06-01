package com.app.usochicamochabackend.fuel.application.dto;

import java.math.BigDecimal;

public record FuelMonthlyStatsResponse(
        int year,
        int month,
        String monthLabel,
        BigDecimal totalGallons,
        BigDecimal totalCost,
        BigDecimal machineGallons,
        BigDecimal machineCost,
        long machineLoads,
        BigDecimal vehicleGallons,
        BigDecimal vehicleCost,
        long vehicleLoads,
        BigDecimal motoGallons,
        BigDecimal motoCost,
        long motoLoads,
        long totalLoads,
        long anomalyCount
) {}
