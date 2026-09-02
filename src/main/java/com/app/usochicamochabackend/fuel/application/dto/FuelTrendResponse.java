package com.app.usochicamochabackend.fuel.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Un punto mensual de la tendencia histórica del dashboard financiero. */
public record FuelTrendResponse(
    LocalDate mes,
    BigDecimal gastoBruto,
    BigDecimal gastoNeto,
    BigDecimal galonesTotal
) {}
