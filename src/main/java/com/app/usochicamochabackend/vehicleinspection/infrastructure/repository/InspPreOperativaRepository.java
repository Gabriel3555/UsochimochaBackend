package com.app.usochicamochabackend.vehicleinspection.infrastructure.repository;

import com.app.usochicamochabackend.vehicleinspection.infrastructure.entity.InspPreOperativaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

@Repository
public interface InspPreOperativaRepository extends JpaRepository<InspPreOperativaEntity, Long> {
    
    @Query("SELECT i FROM InspPreOperativaEntity i WHERE i.idVehiculo = :vehicleId ORDER BY i.fechaRegistro DESC LIMIT 1")
    Optional<InspPreOperativaEntity> findLatestByVehicleId(@Param("vehicleId") Integer vehicleId);

    @Query("SELECT i FROM InspPreOperativaEntity i JOIN i.vehiculo v WHERE v.idTipoVehiculo = :typeId ORDER BY i.fechaRegistro DESC")
    List<InspPreOperativaEntity> findAllByVehicleType(@Param("typeId") Integer typeId);

    @Query("SELECT i FROM InspPreOperativaEntity i JOIN i.vehiculo v JOIN v.tipoVehiculo t WHERE UPPER(t.nombreTipo) != 'MOTOCICLETA' ORDER BY i.fechaRegistro DESC")
    List<InspPreOperativaEntity> findAllNonMoto();

    @Query("SELECT i FROM InspPreOperativaEntity i JOIN i.vehiculo v JOIN v.tipoVehiculo t WHERE UPPER(t.nombreTipo) = UPPER(:typeName) ORDER BY i.fechaRegistro DESC")
    List<InspPreOperativaEntity> findAllByVehicleTypeName(@Param("typeName") String typeName);
}
