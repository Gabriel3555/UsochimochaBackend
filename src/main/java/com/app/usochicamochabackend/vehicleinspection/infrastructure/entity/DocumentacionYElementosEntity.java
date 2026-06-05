package com.app.usochicamochabackend.vehicleinspection.infrastructure.entity;

import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tabla: documentacion_y_elementos
 * Almacena un registro por cada tipo de documento del vehículo:
 * SOAT, TECNOMECANICA, LICENCIA, EXTINTOR (el código usa TECNOMECANICA al persistir tecnomecánica).
 * Campo fecha_vencimiento es NOT NULL en la BD.
 * Varias filas por tipo representan historial; la vigente tiene {@code activo = true}.
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "documentacion_y_elementos")
public class DocumentacionYElementosEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento")
    private Integer idDocumento;

    @Column(name = "id_vehiculo")
    private Integer idVehiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vehiculo", insertable = false, updatable = false)
    private VehicleEntity vehiculo;

    /** Valores esperados: 'SOAT', 'TECNOMECANICA', 'LICENCIA', 'EXTINTOR' (alineado con servicios y seed SQL). */
    @Column(name = "tipo_documento", length = 50)
    private String tipoDocumento;

    /** Nulo para TARJETA_PROPIEDAD (sin vencimiento); obligatorio para demás tipos */
    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    /** Ruta/URL del archivo (imagen o PDF) asociado al documento. */
    @Column(name = "imagen_url", length = 1024)
    private String imagenUrl;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @Column(name = "registrado_por", length = 100)
    private String registradoPor;

    @Column(name = "content_type", length = 120)
    private String contentType;

    /** Estado del documento: Vigente / Próximo a Vencer / Vencido */
    @Column(name = "estadodatos", length = 255)
    private String estadoDatos;

    /**
     * Vigencia del extintor en formato date (derivado de vigenciaExtintor
     * "YYYY-MM")
     */
    @Column(name = "fecha_extintor")
    private LocalDate fechaExtintor;

    @Column(name = "activo")
    private Boolean activo;

    /** Mes/año adicional (usado para extintor) */
    @Column(name = "mesyear")
    private LocalDate mesyear;

    @PrePersist
    public void prePersist() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
        if (activo == null) {
            activo = true;
        }
    }
}
