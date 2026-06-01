package com.app.usochicamochabackend.fuel.application.port;

import com.app.usochicamochabackend.fuel.application.dto.FuelAssetConfigResponse;

import java.util.Optional;

public interface GetFuelAssetConfigUseCase {
    Optional<FuelAssetConfigResponse> getConfig(String assetType, Long assetId);
}
