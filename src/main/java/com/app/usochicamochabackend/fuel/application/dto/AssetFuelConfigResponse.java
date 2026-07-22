package com.app.usochicamochabackend.fuel.application.dto;

import java.math.BigDecimal;

public record AssetFuelConfigResponse(
    Long id,
    Integer vehicleId,
    Long machineId,
    Long fuelTypeDefaultId,
    BigDecimal consumoEstandar,
    String unidadConsumo,
    BigDecimal tanqueCapacidadGal
) {}
