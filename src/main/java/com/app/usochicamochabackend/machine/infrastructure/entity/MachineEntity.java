package com.app.usochicamochabackend.machine.infrastructure.entity;

import com.app.usochicamochabackend.auth.infrastructure.entity.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "machines")
public class MachineEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String model;
    private String belongsTo;
    private LocalDate soat;
    private String brand;
    private LocalDate runt;
    private Boolean status;
    private String numEngine;
    private String numInterIdentification;

    @Column(name = "fuel_tank_capacity_gallons", precision = 8, scale = 3)
    private BigDecimal fuelTankCapacityGallons;

    /** Eficiencia de fábrica. El valor numérico (gal/h, m³/h según la unidad). Null si no se conoce. */
    @Column(name = "factory_efficiency_gal_per_hour", precision = 8, scale = 2)
    private BigDecimal factoryEfficiencyGalPerHour;

    /** Unidad de la eficiencia de fábrica: GAL_PER_HOUR | M3_PER_HOUR. Default: GAL_PER_HOUR. */
    @Column(name = "factory_efficiency_unit", length = 30)
    private String factoryEfficiencyUnit;

    /** Horometro actual (horas de operación). Se actualiza desde la inspección diaria preoperativa. */
    @Column(name = "horometro_actual")
    private Integer horometroActual;
}
