package com.app.usochicamochabackend.update.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Análisis SOS (Scheduled Oil Sampling - Muestreo Programado de Aceite)
 *
 * Para maquinaria pesada, el cambio de aceite puede extenderse o adelantarse
 * basándose en análisis de laboratorio del aceite usado.
 *
 * Si el análisis indica que el aceite aún está en buen estado a las 800 horas,
 * se puede autorizar su uso hasta ese momento.
 * Si indica contaminación, se adelanta el cambio.
 */
@Entity
@Table(name = "oil_analysis_sos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OilAnalysisSosEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_machine", nullable = false)
    private Long machineId;

    @Column(name = "machine_name", nullable = false)
    private String machineName;

    @Column(name = "analysis_date", nullable = false)
    private LocalDateTime analysisDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "oil_type")
    private OilType oilType;              // Tipo de aceite analizado

    @Column(name = "hours_at_analysis")
    private Integer hoursAtAnalysis;      // Horas de motor cuando se tomó la muestra

    @Column(name = "next_change_hours")
    private Integer nextChangeHours;      // Próximo cambio recomendado (por laboratorio)

    @Column(name = "sos_report_url")
    private String sosReportUrl;          // URL del reporte del laboratorio

    @Column(name = "approved_by_mechanic")
    private String approvedByMechanic;    // Mecánico que autorizó basado en reporte

    @Column(columnDefinition = "TEXT")
    private String observations;          // Observaciones adicionales

    @Column(name = "is_approved", nullable = false)
    private Boolean isApproved = false;   // ¿Fue aprobado por supervisor?

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Calcula extensión de horas vs. lo recomendado por defecto
     */
    public Integer getExtendedHours(Integer standardRequirement) {
        if (nextChangeHours == null || standardRequirement == null) {
            return 0;
        }
        return Math.max(0, nextChangeHours - standardRequirement);
    }

    /**
     * ¿El análisis autoriza extender el cambio de aceite?
     */
    public Boolean authorizesExtension(Integer standardRequirement) {
        return nextChangeHours != null && nextChangeHours > standardRequirement;
    }
}
