package com.app.usochicamochabackend.fuel.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FuelMonthlyDiscountRequest(
    LocalDate fechaInicio,
    LocalDate fechaFin,
    BigDecimal monto
) {}
