package com.app.usochicamochabackend.fuel.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FuelDistributionResponse(
    LocalDate fechaInicio,
    LocalDate fechaFin,
    String areaCosto,
    BigDecimal totalGalonesDespachados,
    BigDecimal totalCostoDespachado,
    List<Fila> filas
) {
    public record Fila(
        Long refuelingId,
        LocalDateTime fechaRegistro,
        Integer vehicleId,
        Long machineId,
        Long responsableId,
        Long fuelTypeId,
        String origen,
        BigDecimal horometroKm,
        BigDecimal cantidadDespachada,
        BigDecimal valorDespachado,
        BigDecimal cantidadReintegrada,
        BigDecimal valorReintegro
    ) {}
}
