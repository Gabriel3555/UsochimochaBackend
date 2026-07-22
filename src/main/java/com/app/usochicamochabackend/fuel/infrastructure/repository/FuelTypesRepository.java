package com.app.usochicamochabackend.fuel.infrastructure.repository;

import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelTypesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FuelTypesRepository extends JpaRepository<FuelTypesEntity, Long> {
}
