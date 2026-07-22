package com.app.usochicamochabackend.fuel.infrastructure.repository;

import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelReintegrationsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FuelReintegrationsRepository extends JpaRepository<FuelReintegrationsEntity, Long> {
    List<FuelReintegrationsEntity> findByRefuelingId(Long refuelingId);
}
