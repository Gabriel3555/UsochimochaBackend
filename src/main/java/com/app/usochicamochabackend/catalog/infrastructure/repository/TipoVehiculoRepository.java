package com.app.usochicamochabackend.catalog.infrastructure.repository;

import com.app.usochicamochabackend.catalog.infrastructure.entity.TipoVehiculoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoVehiculoRepository extends JpaRepository<TipoVehiculoEntity, Integer> {
    Optional<TipoVehiculoEntity> findByNombreTipo(String nombreTipo);
    Optional<TipoVehiculoEntity> findByNombreTipoIgnoreCase(String nombreTipo);
}
