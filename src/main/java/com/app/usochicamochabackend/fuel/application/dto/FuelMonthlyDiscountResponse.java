package com.app.usochicamochabackend.fuel.application.dto;

import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelMonthlyDiscountEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FuelMonthlyDiscountResponse(
    Long id,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    BigDecimal monto
) {
    public static FuelMonthlyDiscountResponse from(FuelMonthlyDiscountEntity entity) {
        return new FuelMonthlyDiscountResponse(
                entity.getId(), entity.getFechaInicio(), entity.getFechaFin(), entity.getMonto());
    }
}
