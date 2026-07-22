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
@Table(name = "fuel_purchases")
public class FuelPurchaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "area_costo")
    private String areaCosto;

    @Column(name = "fuel_type_id")
    private Long fuelTypeId;

    private BigDecimal cantidad;

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

    @Column(name = "responsable_id")
    private Long responsableId;

    @Column(name = "fecha_compra")
    private Timestamp fechaCompra;

    private Boolean status;


}
