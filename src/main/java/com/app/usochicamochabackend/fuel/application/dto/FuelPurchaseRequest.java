package com.app.usochicamochabackend.fuel.application.dto;

import java.math.BigDecimal;

public record FuelPurchaseRequest(
    String areaCosto,
    Long fuelTypeId,
    BigDecimal cantidad,
    BigDecimal precioUnitario,
    BigDecimal descuento,
    BigDecimal totalIngresado
) {}
