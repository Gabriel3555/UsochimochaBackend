package com.app.usochicamochabackend.fuel.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FuelDashboardResponse(
    LocalDate fechaInicio,
    LocalDate fechaFin,
    BigDecimal totalComprasAlmacen,
    BigDecimal totalTanqueosBomba,
    BigDecimal totalDescuentos,
    BigDecimal gastoBruto,
    BigDecimal gastoNeto,
    BigDecimal ahorro,
    List<GalonesPorTipo> galonesPorTipo
) {
    public record GalonesPorTipo(Long fuelTypeId, BigDecimal cantidad) {}
}
