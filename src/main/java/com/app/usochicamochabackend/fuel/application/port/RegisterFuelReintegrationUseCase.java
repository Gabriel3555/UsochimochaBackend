package com.app.usochicamochabackend.fuel.application.port;

import com.app.usochicamochabackend.fuel.application.dto.FuelReintegrationRequest;
import com.app.usochicamochabackend.fuel.application.dto.FuelReintegrationResponse;

public interface RegisterFuelReintegrationUseCase {
    FuelReintegrationResponse registrar(FuelReintegrationRequest request, Long responsableId);
}
