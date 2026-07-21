package com.app.usochicamochabackend.maintenance.infrastructure.entity;

import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.catalog.infrastructure.entity.UbicacionEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "mantenimientos")
public class MaintenanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "id_vehiculo")
    private Integer idVehiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vehiculo", insertable = false, updatable = false)
    private VehicleEntity vehiculo;

    @Column(name = "id_ubicacion")
    private Integer idUbicacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ubicacion", insertable = false, updatable = false)
    private UbicacionEntity ubicacion;

    private String responsableAsignado;

    private Integer kilometraje;

    private String tipoMantenimiento; // Preventivo / Correctivo

    @Column(columnDefinition = "TEXT")
    private String repuestosMantenimiento;

    private String tallerResponsable;

    @Column(columnDefinition = "TEXT")
    private String observaciones;
}
