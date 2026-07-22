package com.app.usochicamochabackend.fuel.infrastructure.repository;

import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelTypesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FuelTypesRepository extends JpaRepository<FuelTypesEntity, Long> {
    Optional<FuelTypesEntity> findByCodigo(String codigo);
}
