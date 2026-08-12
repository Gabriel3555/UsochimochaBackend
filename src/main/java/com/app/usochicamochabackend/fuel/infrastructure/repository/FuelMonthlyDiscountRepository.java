package com.app.usochicamochabackend.fuel.infrastructure.repository;

import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelMonthlyDiscountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface FuelMonthlyDiscountRepository extends JpaRepository<FuelMonthlyDiscountEntity, Long> {

    List<FuelMonthlyDiscountEntity> findByStatusTrueOrderByFechaInicioDesc();

    // Solapamiento de intervalos (no coincidencia exacta): un descuento cuenta
    // para el rango consultado si su [fechaInicio, fechaFin] se cruza en algún
    // punto con [:inicio, :fin] — así un descuento "16 jul - 15 ago" sí se
    // incluye si el usuario filtra agosto completo.
    @Query("SELECT COALESCE(SUM(d.monto), 0) FROM FuelMonthlyDiscountEntity d "
            + "WHERE d.status = true AND d.fechaInicio <= :fin AND d.fechaFin >= :inicio")
    BigDecimal sumMontoSolapado(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);
}
