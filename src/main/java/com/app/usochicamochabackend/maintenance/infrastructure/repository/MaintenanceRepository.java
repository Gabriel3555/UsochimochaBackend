package com.app.usochicamochabackend.maintenance.infrastructure.repository;

import com.app.usochicamochabackend.maintenance.infrastructure.entity.MaintenanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceRepository extends JpaRepository<MaintenanceEntity, Long> {
    
    @Query("SELECT m FROM MaintenanceEntity m JOIN m.vehiculo v JOIN v.tipoVehiculo t WHERE t.nombreTipo = :tipoName ORDER BY m.fecha DESC")
    List<MaintenanceEntity> findAllByVehicleTypeName(@Param("tipoName") String tipoName);
}
