package com.app.usochicamochabackend.vehicleinspection.infrastructure.repository;

import com.app.usochicamochabackend.vehicleinspection.infrastructure.entity.InspDetalleDocumentosEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("vehicleInspDetalleDocumentosRepository")
public interface InspDetalleDocumentosRepository extends JpaRepository<InspDetalleDocumentosEntity, Long> {

    /**
     * Retorna el último check de documentos para el vehículo indicado (motos y
     * vehículos)
     */
    @Query(value = """
                SELECT d.* FROM insp_detalle_documentos d
                WHERE d.id_inspeccion = (
                    SELECT MAX(i.id_inspeccion) FROM inspeccion_pre_operativa i
                    WHERE i.id_vehiculo = :idVehiculo
                )
            """, nativeQuery = true)
    Optional<InspDetalleDocumentosEntity> findLatestByVehiculoId(@Param("idVehiculo") Integer idVehiculo);

    Optional<InspDetalleDocumentosEntity> findByIdInspeccion(Long idInspeccion);
}
