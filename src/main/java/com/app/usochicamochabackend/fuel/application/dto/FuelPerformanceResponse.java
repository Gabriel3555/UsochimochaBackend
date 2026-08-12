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
    // true = la alerta se calculó con el rango aprendido del propio historial del
    // activo (promedio ± desviación estándar, ver FuelPerformanceService); false =
    // el activo todavía no tiene las 2 desviaciones previas mínimas, así que se usó
    // la tolerancia fija del 15% (comportamiento de siempre).
    Boolean usaRangoAprendido,
    // Vehículos/motos: "placa — marca" (identifica el vehículo puntual, no solo su
    // categoría). Máquinas no tienen placa — se identifican por su nombre, igual
    // que en el módulo de Maquinaria (no hay categoría formal para ellas).
    String identificacionActivo,
    Boolean esFull
) {}
