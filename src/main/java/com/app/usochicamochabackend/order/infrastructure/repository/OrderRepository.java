package com.app.usochicamochabackend.order.infrastructure.repository;

import com.app.usochicamochabackend.order.infrastructure.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> getAllByInspectionId(Long inspectionId);
    Page<OrderEntity> findAllByInspectionIsNotNull(Pageable pageable);

    @Query("SELECT o FROM OrderEntity o WHERE o.vehicleInspection.idInspeccion = :vehicleInspectionId")
    List<OrderEntity> getAllByVehicleInspectionId(@Param("vehicleInspectionId") Long vehicleInspectionId);

    Page<OrderEntity> findAllByVehicleInspectionIsNotNull(Pageable pageable);

    @Query("SELECT o FROM OrderEntity o WHERE o.vehicleInspection.idVehiculo = :vehicleId")
    List<OrderEntity> findAllByVehicleId(@Param("vehicleId") Integer vehicleId);
}