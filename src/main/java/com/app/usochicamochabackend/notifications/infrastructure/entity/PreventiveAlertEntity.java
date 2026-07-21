package com.app.usochicamochabackend.notifications.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "preventive_alerts")
public class PreventiveAlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_id", nullable = false, length = 50)
    private String assetId;  // placa (vehículos/motos) o machineId

    @Column(name = "asset_type", nullable = false, length = 20)
    private String assetType;  // VEHICULO | MOTOCICLETA | MAQUINARIA | USUARIO

    @Column(name = "alert_type", nullable = false, length = 50)
    private String alertType;  // DOCUMENTO | OIL_CHANGE_VEHICLE | OIL_CHANGE_MACHINE

    @Column(name = "alert_subtype", length = 50)
    private String alertSubtype;  // SOAT, TECNOMECANICA, EXTINTOR, MOTOR, HYDRAULIC, etc.

    @Column(name = "color_estado", nullable = false, length = 20)
    private String colorEstado;  // ROJO | AMARILLO | VERDE

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;  // ACTIVA | RESUELTA

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;  // Para documentos

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "metric_type", length = 20)
    private String metricType;  // DIAS | KILOMETROS | HORAS

    @Column(name = "metric_value")
    private Double metricValue;  // Valor numérico

    @Column(name = "percentage_used")
    private Double percentageUsed;  // % para cambios de aceite

    @Column(name = "status")
    private Boolean status;  // Soft delete

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (status == null) {
            status = true;
        }
    }
}
