package com.app.usochicamochabackend.update.infrastructure.repository;

import com.app.usochicamochabackend.update.infrastructure.entity.OilChangeEntity;
import com.app.usochicamochabackend.update.infrastructure.entity.OilType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OilChangeRepository extends JpaRepository<OilChangeEntity, Long> {
    @Query(value = "SELECT * FROM oil_changes WHERE machine_id = :machineId AND oil_type = 'MOTOR' " +
                   "AND status = true ORDER BY date_stamp DESC LIMIT 1", nativeQuery = true)
    OilChangeEntity getLastMotorOilChangeByMachineId(@Param("machineId") Long machineId);

    @Query(value = "SELECT * FROM oil_changes WHERE machine_id = :machineId AND oil_type = 'HYDRAULIC' " +
                   "AND status = true ORDER BY date_stamp DESC LIMIT 1", nativeQuery = true)
    OilChangeEntity getLastHydraulicOilChangeByMachineId(@Param("machineId") Long machineId);

    // Historial editable ("en caso de error") — no existía ninguna forma de listar
    // más de un registro por máquina/tipo antes de esto, solo "traer el último".
    List<OilChangeEntity> findByMachineIdAndOilTypeAndStatusOrderByDateStampDesc(
            Long machineId, OilType oilType, Boolean status);

    Optional<OilChangeEntity> findByIdAndStatus(Long id, Boolean status);
}