package com.app.usochicamochabackend.fuel.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FuelPurchaseResponse(
    Long id,
    String areaCosto,
    Long fuelTypeId,
    BigDecimal cantidad,
    BigDecimal precioUnitario,
    BigDecimal descuento,
    BigDecimal totalIngresado,
    BigDecimal totalCalculado,
    Boolean discrepanciaValor,
    String urlFactura,
    Long responsableId,
    LocalDateTime fechaCompra
) {}
