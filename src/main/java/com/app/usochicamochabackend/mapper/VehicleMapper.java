package com.app.usochicamochabackend.mapper;

import com.app.usochicamochabackend.vehicle.application.dto.VehicleResponse;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleProjection;

public class VehicleMapper {

    public static VehicleResponse toResponse(VehicleEntity entity) {
        if (entity == null) return null;
        Integer ubiId = entity.getUbicacionBase() != null ? entity.getUbicacionBase().getId() : null;
        String ubiNombre = entity.getUbicacionBase() != null ? entity.getUbicacionBase().getNombreUbicacion() : null;
        return new VehicleResponse(
                entity.getIdVehiculo(),
                entity.getPlaca(),
                entity.getMarca() != null ? entity.getMarca().getDescripcion() : null,
                entity.getIdMarca(),
                entity.getIdTipoVehiculo(),
                entity.getTipoVehiculo() != null ? entity.getTipoVehiculo().getNombreTipo() : null,
                entity.getKilometrajeActual(),
                entity.getBelongsTo(),
                ubiId,
                ubiNombre,
                entity.getActivo(),
                entity.getFuelTankCapacityGallons(),
                entity.getFactoryEfficiencyKmPerGallon(),
                entity.getFactoryEfficiencyUnit()
        );
    }

    public static VehicleResponse toResponse(VehicleProjection projection) {
        if (projection == null) return null;
        return new VehicleResponse(
                projection.getId(),
                projection.getPlaca(),
                projection.getMarca(),
                projection.getIdMarca(),
                projection.getIdTipoVehiculo(),
                projection.getTipoVehiculo(),
                projection.getKilometrajeActual(),
                projection.getBelongsTo(),
                projection.getIdUbicacionBase(),
                projection.getUbicacionBase(),
                null, // VehicleProjection no incluye activo
                null, // VehicleProjection no incluye fuelTankCapacityGallons
                null, // VehicleProjection no incluye factoryEfficiencyKmPerGallon
                null  // VehicleProjection no incluye factoryEfficiencyUnit
        );
    }
}
