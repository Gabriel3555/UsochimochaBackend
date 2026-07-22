package com.app.usochicamochabackend.fuel.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FuelReintegrationResponse(
    Long id,
    Long refuelingId,
    BigDecimal cantidadReintegrada,
    BigDecimal valorReintegro,
    Long responsableId,
    LocalDateTime fechaReintegro
) {}
