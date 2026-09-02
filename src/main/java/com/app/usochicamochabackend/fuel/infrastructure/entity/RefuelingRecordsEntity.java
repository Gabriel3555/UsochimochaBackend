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
@Table(name = "refueling_records")
public class RefuelingRecordsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id")
    private Integer vehicleId;

    @Column(name = "machine_id")
    private Long machineId;

    private String lugar;

    @Column(name = "area_costo")
    private String areaCosto;

    @Column(name = "fuel_type_id")
    private Long fuelTypeId;

    @Column(name = "cantidad_galones")
    private BigDecimal cantidadGalones;

    @Column(name = "horometro_km")
    private BigDecimal horometroKm;

    @Column(name = "es_full")
    private Boolean esFull;

    @Column(name = "precio_unitario")
    private BigDecimal precioUnitario;

    private BigDecimal descuento;

    @Column(name = "total_ingresado")
    private BigDecimal totalIngresado;

    @Column(name = "total_calculado")
    private BigDecimal totalCalculado;

    @Column(name = "discrepancia_valor")
    private Boolean discrepanciaValor;

    @Column(name = "url_factura")
    private String urlFactura;

    private String origen;

    @Column(name = "responsable_id")
    private Long responsableId;

    @Column(name = "fecha_registro")
    private Timestamp fechaRegistro;

    private Boolean status;

}
