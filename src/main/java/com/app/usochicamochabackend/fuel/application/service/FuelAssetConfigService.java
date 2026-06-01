package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.exception.BadRequestException;
import com.app.usochicamochabackend.fuel.application.dto.FuelAssetConfigRequest;
import com.app.usochicamochabackend.fuel.application.dto.FuelAssetConfigResponse;
import com.app.usochicamochabackend.fuel.application.port.GetFuelAssetConfigUseCase;
import com.app.usochicamochabackend.fuel.application.port.UpsertFuelAssetConfigUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.AssetType;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelAssetConfigEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.QuantityUnit;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelAssetConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FuelAssetConfigService implements UpsertFuelAssetConfigUseCase, GetFuelAssetConfigUseCase {

    private final FuelAssetConfigRepository configRepository;

    @Override
    @Transactional
    public FuelAssetConfigResponse upsertConfig(String assetType, Long assetId, FuelAssetConfigRequest request) {
        AssetType type = parseAssetType(assetType);
        QuantityUnit unit = request.defaultUnit() != null
                ? parseUnit(request.defaultUnit())
                : QuantityUnit.LITERS;

        FuelAssetConfigEntity entity = configRepository
                .findByAssetTypeAndAssetId(type, assetId)
                .orElseGet(() -> FuelAssetConfigEntity.builder()
                        .assetType(type)
                        .assetId(assetId)
                        .build());

        entity.setTankCapacityLiters(request.tankCapacityLiters());
        entity.setDefaultUnit(unit);
        entity.setMonthlyBudgetRef(request.monthlyBudgetRef());

        return toResponse(configRepository.save(entity));
    }

    @Override
    public Optional<FuelAssetConfigResponse> getConfig(String assetType, Long assetId) {
        AssetType type = parseAssetType(assetType);
        return configRepository.findByAssetTypeAndAssetId(type, assetId).map(this::toResponse);
    }

    private FuelAssetConfigResponse toResponse(FuelAssetConfigEntity e) {
        return new FuelAssetConfigResponse(
                e.getId(),
                e.getAssetType().name(),
                e.getAssetId(),
                e.getTankCapacityLiters(),
                e.getDefaultUnit() != null ? e.getDefaultUnit().name() : null,
                e.getMonthlyBudgetRef(),
                e.getUpdatedAt()
        );
    }

    private AssetType parseAssetType(String value) {
        try {
            return AssetType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Tipo de activo inválido: " + value);
        }
    }

    private QuantityUnit parseUnit(String value) {
        try {
            return QuantityUnit.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unidad inválida: " + value);
        }
    }
}
