package com.app.usochicamochabackend.fuel.application.port;

import com.app.usochicamochabackend.fuel.application.dto.FuelDistributionResponse;

import java.time.LocalDate;

public interface GetFuelDistributionUseCase {
    FuelDistributionResponse obtenerDistribucion(String area, LocalDate fechaInicio, LocalDate fechaFin);
}
