package com.app.usochicamochabackend.fuel.application.port;

import com.app.usochicamochabackend.fuel.application.dto.AssetFuelConfigRequest;
import com.app.usochicamochabackend.fuel.application.dto.AssetFuelConfigResponse;

import java.util.List;

public interface ManageAssetFuelConfigUseCase {
    List<AssetFuelConfigResponse> listar();
    AssetFuelConfigResponse configurarVehiculo(Integer vehicleId, AssetFuelConfigRequest request);
    AssetFuelConfigResponse configurarMaquina(Long machineId, AssetFuelConfigRequest request);
}
