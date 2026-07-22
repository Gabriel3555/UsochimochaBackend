package com.app.usochicamochabackend.fuel.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FuelWarehouseMovementsResponse(
    LocalDate fechaInicio,
    LocalDate fechaFin,
    List<Conciliacion> conciliacion,
    List<CompraHistorial> historialCompras
) {
    public record Conciliacion(
        String areaCosto,
        Long fuelTypeId,
        BigDecimal saldoInicial,
        BigDecimal entradas,
        BigDecimal salidas,
        BigDecimal saldoFinal
    ) {}

    public record CompraHistorial(
        Long id,
        LocalDateTime fechaCompra,
        String areaCosto,
        Long fuelTypeId,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal descuento,
        BigDecimal totalCalculado,
        String urlFactura
    ) {}
}
