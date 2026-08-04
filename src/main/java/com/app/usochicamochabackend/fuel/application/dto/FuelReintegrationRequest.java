package com.app.usochicamochabackend.fuel.application.dto;

import java.math.BigDecimal;

public record FuelReintegrationRequest(
    Long refuelingId,
    BigDecimal cantidadReintegrada,
    // Opcional — deja constancia de por qué se devolvió el combustible sobrante.
    String motivo
) {}
