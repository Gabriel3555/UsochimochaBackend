package com.app.usochicamochabackend.fuel.infrastructure.repository;

import com.app.usochicamochabackend.fuel.infrastructure.entity.AssetType;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelAssetConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FuelAssetConfigRepository extends JpaRepository<FuelAssetConfigEntity, Long> {
    Optional<FuelAssetConfigEntity> findByAssetTypeAndAssetId(AssetType assetType, Long assetId);
}
