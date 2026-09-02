package com.app.usochicamochabackend.vehicle.infrastructure.entity;

import com.app.usochicamochabackend.catalog.infrastructure.entity.TipoVehiculoEntity;
import com.app.usochicamochabackend.catalog.infrastructure.entity.UbicacionEntity;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.entity.DocumentacionYElementosEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "vehiculos")
public class VehicleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vehiculo")
    private Integer idVehiculo;

    private String placa;

    @Column(name = "id_marca")
    private Integer idMarca;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_marca", insertable = false, updatable = false)
    private MarcaModeloEntity marca;

    @Column(name = "id_tipo_vehiculo")
    private Integer idTipoVehiculo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tipo_vehiculo", insertable = false, updatable = false)
    private TipoVehiculoEntity tipoVehiculo;

    private Boolean activo;

    @Column(name = "kilometraje_actual")
    private Integer kilometrajeActual;

    @Column(name = "fecha_ultimo_reporte")
    private LocalDateTime fechaUltimoReporte;

    @Column(name = "belongs_to")
    private String belongsTo;

    /** Ubicación operativa por defecto (catálogo); la de cada inspección sigue en inspeccion_pre_operativa.id_ubicacion. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ubicacion_base")
    private UbicacionEntity ubicacionBase;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "vehiculo")
    private List<DocumentacionYElementosEntity> documentos;
}
