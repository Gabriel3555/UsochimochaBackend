package com.app.usochicamochabackend.fuel.infrastructure.repository;

import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelPurchaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FuelPurchaseRepository extends JpaRepository<FuelPurchaseEntity, Long> {
}
