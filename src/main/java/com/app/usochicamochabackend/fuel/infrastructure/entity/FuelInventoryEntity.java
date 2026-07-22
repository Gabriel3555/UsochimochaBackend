package com.app.usochicamochabackend.fuel.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "fuel_inventory")
public class FuelInventoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "area_costo")
    private String areaCosto;

    @Column(name = "fuel_type_id")
    private Long fuelTypeId;

    @Column(name = "cantidad_disponible")
    private BigDecimal cantidadDisponible;

    @Column(name = "actualizado_en")
    private Timestamp actualizadoEn;

}
