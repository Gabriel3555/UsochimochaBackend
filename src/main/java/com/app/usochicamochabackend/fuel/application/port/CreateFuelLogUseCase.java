package com.app.usochicamochabackend.fuel.application.port;

import com.app.usochicamochabackend.fuel.application.dto.FuelLogRequest;
import com.app.usochicamochabackend.fuel.application.dto.FuelLogResponse;

public interface CreateFuelLogUseCase {
    FuelLogResponse createFuelLog(FuelLogRequest request);
}
