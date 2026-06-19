package com.app.usochicamochabackend.notifications.infrastructure.entity;

import com.app.usochicamochabackend.auth.infrastructure.entity.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Data
@Entity
@Table(name = "alerts", indexes = {
    @Index(name = "idx_placa", columnList = "placa"),
    @Index(name = "idx_estado", columnList = "estado"),
    @Index(name = "idx_tipo_alerta", columnList = "tipo_alerta"),
    @Index(name = "idx_fecha_vencimiento", columnList = "fecha_vencimiento"),
    @Index(name = "idx_placa_tipo_estado", columnList = "placa,tipo_alerta,estado")
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", unique = true, length = 36)
    private String uuid;

    @Column(name = "placa", length = 20)
    private String placa;

    @Column(name = "machine_id")
    private Long machineId;

    @Column(name = "machine_name", length = 255)
    private String machineName;

    @Column(name = "tipo_maquinaria", length = 50)
    private String tipoMaquinaria;

    @Column(name = "tipo_alerta", nullable = false, length = 50)
    private String tipoAlerta;

    @Column(name = "subtipo", length = 50)
    private String subtipo;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "color_estado", length = 20)
    private String colorEstado;

    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    private UserEntity createdByUser;

    @Column(name = "documento_id")
    private Long documentoId;

    @Column(name = "metric", length = 20)
    private String metric;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }

    /**
     * Calcula el color del estado basado en fecha de vencimiento
     * Para EXTINTOR: usa comparación de meses
     *   ROJO: mes de vencimiento o después (está VENCIDO)
     *   AMARILLO: un mes antes (Próximo a vencer)
     *   VERDE: más de un mes
     * Para otros documentos: usa comparación de días
     *   ROJO: vencido o hoy
     *   AMARILLO: próximos 30 días
     *   VERDE: más de 30 días
     */
    public void calculateColorEstado() {
        if (this.fechaVencimiento == null) {
            this.colorEstado = "VERDE";
            return;
        }

        LocalDate today = LocalDate.now();

        // Lógica especial para EXTINTOR: comparar por meses
        if (this.tipoAlerta != null && this.tipoAlerta.contains("EXTINTOR")) {
            YearMonth mesActual = YearMonth.from(today);
            YearMonth mesVencimiento = YearMonth.from(this.fechaVencimiento);
            YearMonth mesAnterior = mesVencimiento.minusMonths(1);

            if (mesActual.isAfter(mesAnterior)) {
                // Estamos en mes de vencimiento o después
                this.colorEstado = "ROJO";
            } else if (mesActual.equals(mesAnterior)) {
                // Estamos un mes antes
                this.colorEstado = "AMARILLO";
            } else {
                // Más de un mes de diferencia
                this.colorEstado = "VERDE";
            }
        } else {
            // Lógica estándar para otros documentos: por días
            LocalDate threshold30 = today.plusDays(30);

            if (this.fechaVencimiento.isBefore(today) || this.fechaVencimiento.isEqual(today)) {
                this.colorEstado = "ROJO";
            } else if (this.fechaVencimiento.isBefore(threshold30) || this.fechaVencimiento.isEqual(threshold30)) {
                this.colorEstado = "AMARILLO";
            } else {
                this.colorEstado = "VERDE";
            }
        }
    }
}
