package com.app.usochicamochabackend.fuel.application.port;

import com.app.usochicamochabackend.fuel.application.dto.FuelBudgetProjectionRow;
import com.app.usochicamochabackend.fuel.application.dto.FuelDashboardResponse;
import com.app.usochicamochabackend.fuel.application.dto.FuelTrendResponse;

import java.time.LocalDate;
import java.util.List;

public interface GetFuelDashboardUseCase {
    FuelDashboardResponse obtenerDashboard(LocalDate fechaInicio, LocalDate fechaFin);
    List<FuelTrendResponse> obtenerTendencia(Integer meses, LocalDate fechaFin);
    List<FuelBudgetProjectionRow> obtenerProyeccionPresupuestal(LocalDate fechaFin);
}
