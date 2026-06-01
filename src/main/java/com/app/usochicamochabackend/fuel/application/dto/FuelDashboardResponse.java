package com.app.usochicamochabackend.fuel.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record FuelDashboardResponse(
        long totalRecords,
        BigDecimal totalQuantityGallons,
        BigDecimal totalCost,
        long machineCount,
        long vehicleCount,
        long motoCount,
        long anomalyCount,
        long pendingInvoiceCount,
        List<FuelLogResponse> recentLogs
) {}
