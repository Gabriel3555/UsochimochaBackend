package com.app.usochicamochabackend.vehicle.infrastructure.repository;

import com.app.usochicamochabackend.vehicle.infrastructure.entity.MarcaModeloEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarcaModeloRepository extends JpaRepository<MarcaModeloEntity, Integer> {

    boolean existsByDescripcionIgnoreCase(String descripcion);

    boolean existsByDescripcionIgnoreCaseAndIdMarcaNot(String descripcion, Integer idMarca);
}
