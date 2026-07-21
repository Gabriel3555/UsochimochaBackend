package com.app.usochicamochabackend.vehicleinspection.infrastructure.repository;

import com.app.usochicamochabackend.vehicleinspection.infrastructure.entity.DocumentacionYElementosEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentacionYElementosRepository extends JpaRepository<DocumentacionYElementosEntity, Integer> {

    /**
     * Documento vigente del tipo (activo true o null) más reciente por id.
     */
    @Query(value = """
            SELECT * FROM documentacion_y_elementos
            WHERE id_vehiculo = :idVehiculo
              AND tipo_documento = :tipoDocumento
              AND (activo IS NULL OR activo = true)
            ORDER BY id_documento DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<DocumentacionYElementosEntity> findLatestByVehiculoAndTipo(
            @Param("idVehiculo") Integer idVehiculo,
            @Param("tipoDocumento") String tipoDocumento);

    List<DocumentacionYElementosEntity> findByIdVehiculoOrderByIdDocumentoDesc(Integer idVehiculo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DocumentacionYElementosEntity d SET d.activo = false WHERE d.idVehiculo = :idVehiculo "
            + "AND d.tipoDocumento = :tipoDocumento AND (d.activo IS NULL OR d.activo = true)")
    void deactivateAllActiveForVehiculoAndTipo(
            @Param("idVehiculo") Integer idVehiculo,
            @Param("tipoDocumento") String tipoDocumento);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DocumentacionYElementosEntity d SET d.imagenUrl = :url WHERE d.idDocumento = :id")
    void updateImagenUrlById(@Param("id") Integer id, @Param("url") String url);
}
