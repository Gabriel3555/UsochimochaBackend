package com.app.usochicamochabackend.fuel.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FuelPerformanceResponse(
    Long refuelingId,
    Integer vehicleId,
    Long machineId,
    Long fuelTypeId,
    LocalDateTime fechaRegistro,
    BigDecimal horometroAnterior,
    BigDecimal horometroActual,
    BigDecimal ejecutado,
    BigDecimal consumoEstandar,
    BigDecimal galonesProyectados,
    BigDecimal galonesReal,
    BigDecimal diferencia,
    Boolean alerta,
    // Vehículos/motos: "placa — marca" (identifica el vehículo puntual, no solo su
    // categoría). Máquinas no tienen placa — se identifican por su nombre, igual
    // que en el módulo de Maquinaria (no hay categoría formal para ellas).
    String identificacionActivo,
    Boolean esFull
) {}
