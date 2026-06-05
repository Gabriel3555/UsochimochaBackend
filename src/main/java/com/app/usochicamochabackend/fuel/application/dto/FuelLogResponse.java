package com.app.usochicamochabackend.fuel.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FuelLogResponse(
        Long id,
        String assetType,
        Long assetId,
        String assetPlate,
        LocalDateTime fuelDateTime,
        BigDecimal odometerKm,
        BigDecimal hourMeter,
        BigDecimal previousOdometer,
        BigDecimal previousHourMeter,
        BigDecimal deltaKm,
        BigDecimal deltaHours,
        BigDecimal quantity,
        String quantityUnit,
        BigDecimal quantityGallons,
        BigDecimal pricePerUnit,
        BigDecimal totalCostCalculated,
        BigDecimal totalCostActual,
        Boolean totalCostMismatch,
        String fuelType,
        String serviceStation,
        BigDecimal discountAmount,
        String invoicePhotoUrl,
        String invoiceStatus,
        String voucherNumber,
        String notes,
        BigDecimal efficiencyValue,
        String efficiencyUnit,
        Boolean isAnomaly,
        String registeredBy,
        LocalDateTime createdAt,
        String anomalyDismissedBy,
        LocalDateTime anomalyDismissedAt,
        String anomalyDismissReason,
        BigDecimal factoryEfficiency,   // eficiencia de fábrica del activo (km/gal o gal/h)
        String factoryEfficiencyUnit    // unidad: KM_PER_GALLON o GALLON_PER_HOUR
) {}
