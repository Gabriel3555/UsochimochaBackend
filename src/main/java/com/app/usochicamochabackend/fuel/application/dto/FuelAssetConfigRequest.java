package com.app.usochicamochabackend.fuel.application.dto;

import java.math.BigDecimal;

public record FuelAssetConfigRequest(
        BigDecimal tankCapacityLiters,
        String defaultUnit,
        BigDecimal monthlyBudgetRef
) {}
