package com.app.usochicamochabackend.fuel.infrastructure.repository;

import com.app.usochicamochabackend.fuel.infrastructure.entity.AssetFuelConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssetFuelConfigRepository extends JpaRepository<AssetFuelConfigEntity, Long> {
    Optional<AssetFuelConfigEntity> findByVehicleId(Integer vehicleId);
    Optional<AssetFuelConfigEntity> findByMachineId(Long machineId);
}
