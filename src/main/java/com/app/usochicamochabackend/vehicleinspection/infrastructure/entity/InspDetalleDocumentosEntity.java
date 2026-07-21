package com.app.usochicamochabackend.vehicleinspection.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tabla: insp_detalle_documentos
 * Sección 3 — Documentación: check de SOAT / Tecno / Licencia
 * (Vigente / Próximo a Vencer / Vencido)
 */
@Data
@Entity(name = "VehicleInspDetalleDocumentosEntity")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "insp_detalle_documentos")
public class InspDetalleDocumentosEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_doc_check")
    private Long idDocCheck;

    @Column(name = "id_inspeccion")
    private Long idInspeccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_inspeccion", insertable = false, updatable = false)
    private InspPreOperativaEntity inspeccion;

    @Column(name = "check_soat", length = 20)
    private String checkSoat;

    @Column(name = "check_tecno", length = 20)
    private String checkTecno;

    @Column(name = "check_extintor", length = 20)
    private String checkExtintor;
}
