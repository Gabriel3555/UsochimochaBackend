package com.app.usochicamochabackend.fuel.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "asset_fuel_config")
public class AssetFuelConfigEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id")
    private Integer vehicleId;

    @Column(name = "machine_id")
    private Long machineId;

    @Column(name = "consumo_estandar")
    private BigDecimal consumoEstandar;

    @Column(name = "unidad_consumo")
    private String unidadConsumo;

    @Column(name = "fuel_type_default_id")
    private Long fuelTypeDefaultId;

    @Column(name = "tanque_capacidad_gal")
    private BigDecimal  tanqueCapacidadGal;

}
