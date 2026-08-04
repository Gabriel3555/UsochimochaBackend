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
@Table(name = "fuel_reintegrations")
public class FuelReintegrationsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "refueling_id")
    private Long refuelingId;

    @Column(name = "cantidad_reintegrada")
    private BigDecimal cantidadReintegrada;

    @Column(name = "valor_reintegro")
    private BigDecimal valorReintegro;

    @Column(name = "motivo")
    private String motivo;

    @Column(name = "responsable_id")
    private Long responsableId;

    @Column(name = "fecha_reintegro")
    private Timestamp fechaReintegro;



}
