package com.app.usochicamochabackend.fuel.application.port;

import com.app.usochicamochabackend.fuel.application.dto.FuelPerformanceResponse;

import java.time.LocalDate;
import java.util.List;

public interface GetFuelPerformanceUseCase {
    List<FuelPerformanceResponse> obtenerRendimiento(String tipo, LocalDate fechaInicio, LocalDate fechaFin);
}
