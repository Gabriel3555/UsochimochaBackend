package com.app.usochicamochabackend.notifications.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad para auditoría de alertas de cambio de aceite.
 *
 * Registra cada alerta enviada por WebSocket para:
 * - Trazabilidad
 * - Análisis de patrones
 * - Reportes de SLA
 * - Debugging
 */
@Entity
@Table(name = "oil_change_alerts", indexes = {
    @Index(name = "idx_oil_alerts_placa", columnList = "placa"),
    @Index(name = "idx_oil_alerts_color", columnList = "alert_color"),
    @Index(name = "idx_oil_alerts_tipo", columnList = "tipo_maquinaria"),
    @Index(name = "idx_oil_alerts_timestamp", columnList = "created_at DESC"),
    @Index(name = "idx_oil_alerts_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OilChangeAlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", unique = true, nullable = false, length = 36)
    private String uuid;

    @Column(name = "placa", nullable = false, length = 20)
    private String placa;

    @Column(name = "tipo_maquinaria", nullable = false, length = 50)
    private String tipoMaquinaria;  // "VEHICULO", "MOTOCICLETA", "MAQUINARIA"

    @Column(name = "alert_color", nullable = false, length = 20)
    private String alertColor;  // "GREEN", "BLUE", "YELLOW", "RED"

    @Column(name = "alert_status", length = 100)
    private String alertStatus;  // "OK", "Programado", "Próximo a Cambio", "Cambio de Aceite"

    @Column(name = "percentage_used")
    private Double percentageUsed;  // 0-100+ %

    @Column(name = "distance_remaining")
    private Long distanceRemaining;  // km o horas restantes

    @Column(name = "alert_message", columnDefinition = "TEXT")
    private String alertMessage;

    @Column(name = "notification_sent_at")
    private LocalDateTime notificationSentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "status", nullable = false)
    private Boolean status = true;  // Soft delete

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (notificationSentAt == null) {
            notificationSentAt = LocalDateTime.now();
        }
    }
}
