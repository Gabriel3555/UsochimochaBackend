package com.app.usochicamochabackend.update.infrastructure.repository;

import com.app.usochicamochabackend.update.infrastructure.entity.OilChangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OilChangeRepository extends JpaRepository<OilChangeEntity, Long> {
    @Query(value = "SELECT * FROM oil_changes WHERE machine_id = :machineId AND oil_type = 'MOTOR' " +
                   "ORDER BY date_stamp DESC LIMIT 1", nativeQuery = true)
    OilChangeEntity getLastMotorOilChangeByMachineId(@Param("machineId") Long machineId);

    @Query(value = "SELECT * FROM oil_changes WHERE machine_id = :machineId AND oil_type = 'HYDRAULIC' " +
                   "ORDER BY date_stamp DESC LIMIT 1", nativeQuery = true)
    OilChangeEntity getLastHydraulicOilChangeByMachineId(@Param("machineId") Long machineId);
}