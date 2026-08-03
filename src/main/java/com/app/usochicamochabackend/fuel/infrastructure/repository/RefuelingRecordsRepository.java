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

    Optional<RefuelingRecordsEntity> findByIdAndStatus(Long id, Boolean status);

    @Query("SELECT COALESCE(SUM(r.totalCalculado), 0) FROM RefuelingRecordsEntity r "
            + "WHERE r.lugar = 'BOMBA' AND r.fechaRegistro BETWEEN :inicio AND :fin")
    BigDecimal sumTotalCalculadoBombaBetween(@Param("inicio") Timestamp inicio, @Param("fin") Timestamp fin);

    // Descuento aplicado en tanqueos de BOMBA — totalCalculado ya lo resta (ver
    // RefuelingRecordService), así que esto sirve para reconstruir el bruto (neto +
    // descuento) y para que "ahorro" del dashboard no ignore los descuentos de bomba.
    @Query("SELECT COALESCE(SUM(r.descuento), 0) FROM RefuelingRecordsEntity r "
            + "WHERE r.lugar = 'BOMBA' AND r.fechaRegistro BETWEEN :inicio AND :fin")
    BigDecimal sumDescuentoBombaBetween(@Param("inicio") Timestamp inicio, @Param("fin") Timestamp fin);

    @Query("SELECT r.fuelTypeId, COALESCE(SUM(r.cantidadGalones), 0) FROM RefuelingRecordsEntity r "
            + "WHERE r.fechaRegistro BETWEEN :inicio AND :fin GROUP BY r.fuelTypeId")
    List<Object[]> sumCantidadPorTipoBetween(@Param("inicio") Timestamp inicio, @Param("fin") Timestamp fin);

    @Query("SELECT r.fuelTypeId, COALESCE(SUM(r.totalCalculado), 0) FROM RefuelingRecordsEntity r "
            + "WHERE r.lugar = 'BOMBA' AND r.fechaRegistro BETWEEN :inicio AND :fin GROUP BY r.fuelTypeId")
    List<Object[]> sumTotalCalculadoBombaPorTipoBetween(@Param("inicio") Timestamp inicio, @Param("fin") Timestamp fin);

    // Cantidad SOLO de bomba por tipo — separada de sumCantidadPorTipoBetween (que
    // mezcla bomba+almacén) para poder calcular un precio/unidad real (gasto bomba
    // ÷ cantidad bomba) sin diluirlo con galones de almacén, que no tienen costo.
    @Query("SELECT r.fuelTypeId, COALESCE(SUM(r.cantidadGalones), 0) FROM RefuelingRecordsEntity r "
            + "WHERE r.lugar = 'BOMBA' AND r.fechaRegistro BETWEEN :inicio AND :fin GROUP BY r.fuelTypeId")
    List<Object[]> sumCantidadBombaPorTipoBetween(@Param("inicio") Timestamp inicio, @Param("fin") Timestamp fin);

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

    List<RefuelingRecordsEntity> findByFechaRegistroBetween(Timestamp inicio, Timestamp fin);

    long countByDiscrepanciaValorTrueAndFechaRegistroBetween(Timestamp inicio, Timestamp fin);

    // Tipos de combustible con al menos un tanqueo alguna vez — ver nota en
    // FuelPurchaseRepository.findDistinctFuelTypeIds().
    @Query("SELECT DISTINCT r.fuelTypeId FROM RefuelingRecordsEntity r")
    List<Long> findDistinctFuelTypeIds();
}
