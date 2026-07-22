package com.app.usochicamochabackend.fuel.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FuelPerformanceResponse(
    Long refuelingId,
    Integer vehicleId,
    Long machineId,
    LocalDateTime fechaRegistro,
    BigDecimal horometroAnterior,
    BigDecimal horometroActual,
    BigDecimal ejecutado,
    BigDecimal consumoEstandar,
    BigDecimal galonesProyectados,
    BigDecimal galonesReal,
    BigDecimal diferencia,
    Boolean alerta
) {}
