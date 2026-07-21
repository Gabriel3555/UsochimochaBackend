package com.app.usochicamochabackend.update.infrastructure.repository;

import com.app.usochicamochabackend.update.infrastructure.entity.VehicleOilChangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleOilChangeRepository extends JpaRepository<VehicleOilChangeEntity, Long> {

    Optional<VehicleOilChangeEntity> findFirstByVehicleIdVehiculoOrderByDateStampDesc(Integer vehicleId);

    @Query("SELECT v FROM VehicleOilChangeEntity v WHERE v.vehicle.placa = :placa ORDER BY v.dateStamp DESC")
    List<VehicleOilChangeEntity> findAllByPlacaOrderByDateStampDesc(@Param("placa") String placa);
}
