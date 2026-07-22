package com.app.usochicamochabackend.fuel.infrastructure.repository;

import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelInventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FuelInventoryRepository extends JpaRepository<FuelInventoryEntity, Long> {
}
