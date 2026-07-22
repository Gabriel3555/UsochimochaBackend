package com.app.usochicamochabackend.fuel.application.dto;

import java.math.BigDecimal;

public record FuelReintegrationRequest(
    Long refuelingId,
    BigDecimal cantidadReintegrada
) {}
