package com.app.usochicamochabackend.fuel.infrastructure.repository;

import com.app.usochicamochabackend.fuel.infrastructure.entity.AssetType;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelStatsMonthlyEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FuelStatsMonthlyRepository extends JpaRepository<FuelStatsMonthlyEntity, Long> {

    List<FuelStatsMonthlyEntity> findByYearMonthIn(List<String> yearMonths);

    Optional<FuelStatsMonthlyEntity> findByYearMonthAndAssetTypeAndFuelType(
            String yearMonth, AssetType assetType, FuelType fuelType);
}
