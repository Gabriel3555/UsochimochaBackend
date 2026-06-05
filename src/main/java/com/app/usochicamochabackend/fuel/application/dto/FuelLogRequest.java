package com.app.usochicamochabackend.fuel.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FuelLogRequest(
        String syncId,
        String assetType,
        Long assetId,
        String assetPlate,
        LocalDateTime fuelDateTime,
        BigDecimal odometerKm,
        BigDecimal hourMeter,
        BigDecimal quantity,
        String quantityUnit,
        BigDecimal pricePerUnit,
        BigDecimal totalCostActual,
        String fuelType,
        String serviceStation,
        BigDecimal discountAmount,
        String invoicePhotoUrl,
        String invoicePhotoBase64,
        String invoiceFileName,
        String voucherNumber,
        String notes
) {}
