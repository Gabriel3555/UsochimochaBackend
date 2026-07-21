package com.app.usochicamochabackend.fuel.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FuelAssetConfigResponse(
        Long id,
        String assetType,
        Long assetId,
        BigDecimal tankCapacityLiters,
        String defaultUnit,
        BigDecimal monthlyBudgetRef,
        LocalDateTime updatedAt
) {}
