package com.app.usochicamochabackend.catalog.infrastructure.repository;

import com.app.usochicamochabackend.catalog.infrastructure.entity.UbicacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UbicacionRepository extends JpaRepository<UbicacionEntity, Integer> {
    List<UbicacionEntity> findByActivoTrue();

    boolean existsByNombreUbicacionIgnoreCase(String nombreUbicacion);

    boolean existsByNombreUbicacionIgnoreCaseAndIdNot(String nombreUbicacion, Integer id);
}
