package com.app.usochicamochabackend.machine.infrastructure.entity;

import com.app.usochicamochabackend.auth.infrastructure.entity.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    /** Horometro actual (horas de operación). Se actualiza desde la inspección diaria preoperativa. */
    @Column(name = "horometro_actual")
    private Integer horometroActual;
}
