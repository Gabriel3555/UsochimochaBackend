package com.app.usochicamochabackend.fuel.application.port;

import com.app.usochicamochabackend.fuel.application.dto.FuelWarehouseBalanceResponse;
import com.app.usochicamochabackend.fuel.application.dto.FuelWarehouseMovementsResponse;

import java.time.LocalDate;

public interface GetFuelWarehouseUseCase {
    FuelWarehouseBalanceResponse obtenerSaldos();
    FuelWarehouseMovementsResponse obtenerMovimientos(LocalDate fechaInicio, LocalDate fechaFin);
}
