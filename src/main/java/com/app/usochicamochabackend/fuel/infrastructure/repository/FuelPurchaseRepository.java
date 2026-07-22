package com.app.usochicamochabackend.fuel.infrastructure.repository;

import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelPurchaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

public interface FuelPurchaseRepository extends JpaRepository<FuelPurchaseEntity, Long> {

    @Query("SELECT COALESCE(SUM(f.totalCalculado), 0) FROM FuelPurchaseEntity f "
            + "WHERE f.fechaCompra BETWEEN :inicio AND :fin")
    BigDecimal sumTotalCalculadoBetween(@Param("inicio") Timestamp inicio, @Param("fin") Timestamp fin);

    @Query("SELECT COALESCE(SUM(f.descuento), 0) FROM FuelPurchaseEntity f "
            + "WHERE f.fechaCompra BETWEEN :inicio AND :fin")
    BigDecimal sumDescuentoBetween(@Param("inicio") Timestamp inicio, @Param("fin") Timestamp fin);

    @Query("SELECT f.fuelTypeId, COALESCE(SUM(f.cantidad), 0) FROM FuelPurchaseEntity f "
            + "WHERE f.fechaCompra BETWEEN :inicio AND :fin GROUP BY f.fuelTypeId")
    List<Object[]> sumCantidadPorTipoBetween(@Param("inicio") Timestamp inicio, @Param("fin") Timestamp fin);

    // Las entradas de la conciliación de almacén (Task 11) deben separarse por área de costo:
    // los inventarios de DISTRITO y ASOCIACION son independientes desde Fase 0-1.
    @Query("SELECT f.areaCosto, f.fuelTypeId, COALESCE(SUM(f.cantidad), 0) FROM FuelPurchaseEntity f "
            + "WHERE f.fechaCompra BETWEEN :inicio AND :fin GROUP BY f.areaCosto, f.fuelTypeId")
    List<Object[]> sumCantidadPorAreaYTipoBetween(@Param("inicio") Timestamp inicio, @Param("fin") Timestamp fin);

    List<FuelPurchaseEntity> findByFechaCompraBetweenOrderByFechaCompraDesc(Timestamp inicio, Timestamp fin);
}
