package com.app.usochicamochabackend.fuel.infrastructure.repository;

import com.app.usochicamochabackend.fuel.infrastructure.entity.AssetType;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelLogEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.InvoiceStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FuelLogRepository extends JpaRepository<FuelLogEntity, Long> {

    Optional<FuelLogEntity> findFirstByAssetTypeAndAssetIdOrderByFuelDateTimeDesc(
            AssetType assetType, Long assetId);

    List<FuelLogEntity> findByAssetTypeAndAssetIdOrderByFuelDateTimeDesc(
            AssetType assetType, Long assetId);

    @Query("SELECT f FROM FuelLogEntity f WHERE f.assetType = :at AND f.assetId = :id AND f.fuelDateTime BETWEEN :from AND :to ORDER BY f.fuelDateTime DESC")
    List<FuelLogEntity> findByAssetAndDateRange(
            @Param("at") AssetType at,
            @Param("id") Long id,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    List<FuelLogEntity> findAllByOrderByFuelDateTimeDesc();

    @Query("SELECT f FROM FuelLogEntity f WHERE f.fuelDateTime BETWEEN :from AND :to ORDER BY f.fuelDateTime DESC")
    List<FuelLogEntity> findAllByDateRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    List<FuelLogEntity> findTop2ByAssetTypeAndAssetIdAndIsFullTankTrueOrderByFuelDateTimeDesc(
            AssetType assetType, Long assetId);

    @Query("SELECT COALESCE(SUM(f.quantityLiters), 0) FROM FuelLogEntity f WHERE f.assetType = :at AND f.assetId = :id AND f.fuelDateTime > :after AND f.fuelDateTime <= :before")
    BigDecimal sumQuantityLitersBetween(
            @Param("at") AssetType at,
            @Param("id") Long id,
            @Param("after") LocalDateTime after,
            @Param("before") LocalDateTime before);

    @Query("SELECT f.efficiencyValue FROM FuelLogEntity f WHERE f.assetType = :at AND f.assetId = :id AND f.isFullTank = true AND f.efficiencyValue IS NOT NULL AND f.id != :excludeId ORDER BY f.fuelDateTime DESC")
    List<BigDecimal> findRecentEfficiencies(
            @Param("at") AssetType at,
            @Param("id") Long id,
            @Param("excludeId") Long excludeId,
            Pageable pageable);

    Optional<FuelLogEntity> findBySyncId(String syncId);

    List<FuelLogEntity> findByIsAnomalyTrueOrderByFuelDateTimeDesc();

    List<FuelLogEntity> findByInvoiceStatusOrderByFuelDateTimeDesc(InvoiceStatus status);
}
