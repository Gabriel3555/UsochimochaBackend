package com.app.usochicamochabackend.catalog.infrastructure.repository;

import com.app.usochicamochabackend.catalog.infrastructure.entity.AreaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AreaRepository extends JpaRepository<AreaEntity, Integer> {
    List<AreaEntity> findByActivoTrue();

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndIdNot(String nombre, Integer id);
}
