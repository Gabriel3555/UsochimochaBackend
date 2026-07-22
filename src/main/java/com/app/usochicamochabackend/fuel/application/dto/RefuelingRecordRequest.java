package com.app.usochicamochabackend.fuel.application.dto;

import java.math.BigDecimal;

public record RefuelingRecordRequest(
    Integer vehicleId,
    Long machineId,
    String lugar,
    String areaCosto,
    Long fuelTypeId,
    BigDecimal cantidadGalones,
    BigDecimal horometroKm,
    Boolean esFull,
    BigDecimal precioUnitario,
    BigDecimal descuento,
    BigDecimal totalIngresado,
    String origen
) {}
