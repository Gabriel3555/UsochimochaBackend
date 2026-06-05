package com.app.usochicamochabackend.mapper;

import com.app.usochicamochabackend.vehicle.application.dto.VehicleResponse;
import com.app.usochicamochabackend.vehicle.application.dto.VehicleResponse.SoatInfo;
import com.app.usochicamochabackend.vehicle.application.dto.VehicleResponse.TecnoInfo;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleProjection;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.entity.DocumentacionYElementosEntity;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Slf4j
public class VehicleMapper {

    public static VehicleResponse toResponse(VehicleEntity entity) {
        if (entity == null) return null;
        Integer ubiId = entity.getUbicacionBase() != null ? entity.getUbicacionBase().getId() : null;
        String ubiNombre = entity.getUbicacionBase() != null ? entity.getUbicacionBase().getNombreUbicacion() : null;
        log.info("📋 Mapeando vehículo {} - belongsTo: '{}' (null={})", entity.getPlaca(), entity.getBelongsTo(), entity.getBelongsTo() == null);

        SoatInfo soatInfo = extractDocumentInfo(entity, "SOAT");
        TecnoInfo tecnoInfo = extractDocumentInfo(entity, "TECNOMECANICA");

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
                entity.getFactoryEfficiencyUnit(),
                soatInfo,
                tecnoInfo
        );
    }

    private static <T> T extractDocumentInfo(VehicleEntity entity, String tipoDocumento) {
        if (entity.getDocumentos() == null || entity.getDocumentos().isEmpty()) {
            return null;
        }

        DocumentacionYElementosEntity doc = entity.getDocumentos().stream()
                .filter(d -> tipoDocumento.equals(d.getTipoDocumento()) && Boolean.TRUE.equals(d.getActivo()))
                .findFirst()
                .orElse(null);

        if (doc == null) {
            return null;
        }

        LocalDate fechaVencimiento = doc.getFechaVencimiento();
        if (fechaVencimiento == null) {
            return null;
        }

        Long diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), fechaVencimiento);
        String estado = diasRestantes < 0 ? "Vencido" : diasRestantes <= 30 ? "Próximo a Vencer" : "Vigente";

        if ("SOAT".equals(tipoDocumento)) {
            return (T) new SoatInfo(fechaVencimiento, diasRestantes, estado);
        } else if ("TECNOMECANICA".equals(tipoDocumento)) {
            return (T) new TecnoInfo(fechaVencimiento, diasRestantes, estado);
        }

        return null;
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
                null, // VehicleProjection no incluye factoryEfficiencyUnit
                null, // VehicleProjection no incluye documentos
                null  // VehicleProjection no incluye documentos
        );
    }
}
