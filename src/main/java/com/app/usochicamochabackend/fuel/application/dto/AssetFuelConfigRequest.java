package com.app.usochicamochabackend.fuel.application.dto;

import java.math.BigDecimal;

public record AssetFuelConfigRequest(
    Long fuelTypeDefaultId,
    BigDecimal consumoEstandar,
    String unidadConsumo,
    BigDecimal tanqueCapacidadGal
) {}
