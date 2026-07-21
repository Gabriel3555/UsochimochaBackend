package com.app.usochicamochabackend.fuel.application.port;

import com.app.usochicamochabackend.fuel.application.dto.FuelAssetConfigRequest;
import com.app.usochicamochabackend.fuel.application.dto.FuelAssetConfigResponse;

public interface UpsertFuelAssetConfigUseCase {
    FuelAssetConfigResponse upsertConfig(String assetType, Long assetId, FuelAssetConfigRequest request);
}
