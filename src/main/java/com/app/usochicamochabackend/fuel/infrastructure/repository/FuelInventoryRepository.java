package com.app.usochicamochabackend.fuel.infrastructure.repository;

import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelInventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FuelInventoryRepository extends JpaRepository<FuelInventoryEntity, Long> {
    Optional<FuelInventoryEntity> findByAreaCostoAndFuelTypeId(String areaCosto, Long fuelTypeId);
}
