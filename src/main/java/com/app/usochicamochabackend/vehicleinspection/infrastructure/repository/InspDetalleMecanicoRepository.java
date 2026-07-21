package com.app.usochicamochabackend.vehicleinspection.infrastructure.repository;

import com.app.usochicamochabackend.vehicleinspection.infrastructure.entity.InspDetalleMecanicoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InspDetalleMecanicoRepository extends JpaRepository<InspDetalleMecanicoEntity, Long> {
    Optional<InspDetalleMecanicoEntity> findByIdInspeccion(Long idInspeccion);
}
