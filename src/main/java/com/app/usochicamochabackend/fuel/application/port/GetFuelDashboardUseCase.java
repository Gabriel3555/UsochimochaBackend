package com.app.usochicamochabackend.fuel.application.port;

import com.app.usochicamochabackend.fuel.application.dto.FuelDashboardResponse;

import java.time.LocalDate;

public interface GetFuelDashboardUseCase {
    FuelDashboardResponse obtenerDashboard(LocalDate fechaInicio, LocalDate fechaFin);
}
