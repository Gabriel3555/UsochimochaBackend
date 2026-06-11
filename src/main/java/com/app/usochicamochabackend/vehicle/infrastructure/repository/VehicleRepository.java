package com.app.usochicamochabackend.vehicle.infrastructure.repository;

import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<VehicleEntity, Integer> {

    @Query("SELECT v FROM VehicleEntity v WHERE v.activo = true ORDER BY v.placa")
    List<VehicleEntity> findAllActiveVehiclesWithDocuments();

    @Query("SELECT v FROM VehicleEntity v WHERE v.activo = true AND UPPER(TRIM(v.tipoVehiculo.nombreTipo)) = UPPER(TRIM(:tipoName)) ORDER BY v.placa")
    List<VehicleEntity> findAllActiveVehiclesByTipoNameWithDocuments(@Param("tipoName") String tipoName);

    @Query(nativeQuery = true, value = """
            SELECT
                v.id_vehiculo        AS "id",
                v.placa              AS "placa",
                m.descripcion        AS "marca",
                v.id_marca           AS "idMarca",
                v.id_tipo_vehiculo   AS "idTipoVehiculo",
                t.nombre_tipo        AS "tipoVehiculo",
                v.kilometraje_actual AS "kilometrajeActual",
                v.belongs_to         AS "belongsTo",
                v.id_ubicacion_base  AS "idUbicacionBase",
                ub.nombre_ubicacion  AS "ubicacionBase"
            FROM vehiculos v
            JOIN cat_marcas_modelos  m ON v.id_marca          = m.id_marca
            JOIN cat_tipos_vehiculo  t ON v.id_tipo_vehiculo  = t.id_tipo_vehiculo
            LEFT JOIN cat_ubicaciones ub ON v.id_ubicacion_base = ub.id_ubicacion
            WHERE v.activo = TRUE
            ORDER BY v.placa
            """)
    List<VehicleProjection> findAllActiveVehicles();

    /** Listado activo filtrado por nombre de tipo (p. ej. MOTOCICLETA) — JOIN de ubicación base como {@link #findAllActiveVehicles()}. */
    @Query(nativeQuery = true, value = """
            SELECT
                v.id_vehiculo        AS "id",
                v.placa              AS "placa",
                m.descripcion        AS "marca",
                v.id_marca           AS "idMarca",
                v.id_tipo_vehiculo   AS "idTipoVehiculo",
                t.nombre_tipo        AS "tipoVehiculo",
                v.kilometraje_actual AS "kilometrajeActual",
                v.belongs_to         AS "belongsTo",
                v.id_ubicacion_base  AS "idUbicacionBase",
                ub.nombre_ubicacion  AS "ubicacionBase"
            FROM vehiculos v
            JOIN cat_marcas_modelos  m ON v.id_marca          = m.id_marca
            JOIN cat_tipos_vehiculo  t ON v.id_tipo_vehiculo  = t.id_tipo_vehiculo
            LEFT JOIN cat_ubicaciones ub ON v.id_ubicacion_base = ub.id_ubicacion
            WHERE v.activo = TRUE
              AND UPPER(TRIM(t.nombre_tipo)) = UPPER(TRIM(:tipoName))
            ORDER BY v.placa
            """)
    List<VehicleProjection> findAllActiveVehiclesByTipoName(@Param("tipoName") String tipoName);

    /**
     * Actualiza el kilometraje del vehículo al guardar una inspección
     * pre-operativa.
     */
    @Modifying
    @Query(nativeQuery = true, value = "UPDATE vehiculos SET kilometraje_actual = :km WHERE id_vehiculo = :idVehiculo")
    void updateKilometraje(@Param("idVehiculo") Integer idVehiculo, @Param("km") Integer km);

    /** Busca el vehículo por placa — el Android envía la placa, no el id. */
    java.util.Optional<VehicleEntity> findByPlaca(String placa);

    /**
     * Busca un vehículo activo por placa con todos sus datos.
     * Retorna null si no existe.
     */
    @Query("SELECT v FROM VehicleEntity v WHERE v.placa = :placa")
    java.util.Optional<VehicleEntity> findVehicleDetailByPlaca(@Param("placa") String placa);

    @Query("SELECT v FROM VehicleEntity v JOIN v.tipoVehiculo t WHERE UPPER(TRIM(t.nombreTipo)) = UPPER(TRIM(:tipoName)) AND v.activo = TRUE")
    List<VehicleEntity> findAllByTipoName(@Param("tipoName") String tipoName);

    @Query("SELECT v FROM VehicleEntity v JOIN v.tipoVehiculo t WHERE UPPER(TRIM(t.nombreTipo)) <> UPPER(TRIM(:tipoName)) AND v.activo = TRUE")
    List<VehicleEntity> findAllByTipoNameNot(@Param("tipoName") String tipoName);

    java.util.Optional<VehicleEntity> findByPlacaAndActivoTrue(String placa);

    @Modifying
    @Query("UPDATE VehicleEntity v SET v.kilometrajeActual = :km, v.fechaUltimoReporte = :fecha WHERE v.idVehiculo = :id")
    void updateKilometrajeWithDate(@Param("id") Integer id, @Param("km") Integer km, @Param("fecha") java.time.LocalDateTime fecha);
}
