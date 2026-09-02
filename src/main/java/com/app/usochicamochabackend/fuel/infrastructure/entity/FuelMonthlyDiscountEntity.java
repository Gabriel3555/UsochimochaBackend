package com.app.usochicamochabackend.fuel.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "fuel_monthly_discounts")
public class FuelMonthlyDiscountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    private BigDecimal monto;

    @Column(name = "responsable_id")
    private Long responsableId;

    @Column(name = "fecha_registro")
    private Timestamp fechaRegistro;

    private Boolean status;
}
