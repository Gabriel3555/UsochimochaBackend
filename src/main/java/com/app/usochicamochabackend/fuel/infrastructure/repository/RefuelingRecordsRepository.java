package com.app.usochicamochabackend.fuel.infrastructure.repository;

import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RefuelingRecordsRepository extends JpaRepository<RefuelingRecordsEntity, Long> {
    Page<RefuelingRecordsEntity> findByStatus(Boolean status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(r.totalCalculado), 0) FROM RefuelingRecordsEntity r "
            + "WHERE r.lugar = 'BOMBA' AND r.fechaRegistro BETWEEN :inicio AND :fin")
    BigDecimal sumTotalCalculadoBombaBetween(@Param("inicio") Timestamp inicio, @Param("fin") Timestamp fin);

    @Query("SELECT r.fuelTypeId, COALESCE(SUM(r.cantidadGalones), 0) FROM RefuelingRecordsEntity r "
            + "WHERE r.fechaRegistro BETWEEN :inicio AND :fin GROUP BY r.fuelTypeId")
    List<Object[]> sumCantidadPorTipoBetween(@Param("inicio") Timestamp inicio, @Param("fin") Timestamp fin);

    // Las salidas de la conciliación de almacén (Task 11) deben separarse por área de costo,
    // mismo motivo que sumCantidadPorAreaYTipoBetween en FuelPurchaseRepository.
    @Query("SELECT r.areaCosto, r.fuelTypeId, COALESCE(SUM(r.cantidadGalones), 0) FROM RefuelingRecordsEntity r "
            + "WHERE r.lugar = 'ALMACEN' AND r.fechaRegistro BETWEEN :inicio AND :fin GROUP BY r.areaCosto, r.fuelTypeId")
    List<Object[]> sumCantidadAlmacenPorAreaYTipoBetween(@Param("inicio") Timestamp inicio, @Param("fin") Timestamp fin);

    @Query("SELECT r FROM RefuelingRecordsEntity r WHERE r.vehicleId = :vehicleId "
            + "AND r.fechaRegistro < :antesDe ORDER BY r.fechaRegistro DESC LIMIT 1")
    Optional<RefuelingRecordsEntity> findAnteriorPorVehicleId(@Param("vehicleId") Integer vehicleId, @Param("antesDe") Timestamp antesDe);

    @Query("SELECT r FROM RefuelingRecordsEntity r WHERE r.machineId = :machineId "
            + "AND r.fechaRegistro < :antesDe ORDER BY r.fechaRegistro DESC LIMIT 1")
    Optional<RefuelingRecordsEntity> findAnteriorPorMachineId(@Param("machineId") Long machineId, @Param("antesDe") Timestamp antesDe);

    List<RefuelingRecordsEntity> findByAreaCostoAndFechaRegistroBetween(String areaCosto, Timestamp inicio, Timestamp fin);

    List<RefuelingRecordsEntity> findByMachineIdIsNotNullAndFechaRegistroBetween(Timestamp inicio, Timestamp fin);

    List<RefuelingRecordsEntity> findByVehicleIdIsNotNullAndFechaRegistroBetween(Timestamp inicio, Timestamp fin);
}
