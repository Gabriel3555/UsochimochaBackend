package com.app.usochicamochabackend.fuel.application.dto;

import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefuelingRecordResponse(
    Long id,
    Integer vehicleId,
    Long machineId,
    String lugar,
    String areaCosto,
    Long fuelTypeId,
    BigDecimal cantidadGalones,
    BigDecimal horometroKm,
    Boolean esFull,
    BigDecimal precioUnitario,
    BigDecimal descuento,
    BigDecimal totalIngresado,
    BigDecimal totalCalculado,
    Boolean discrepanciaValor,
    String urlFactura,
    String origen,
    Long responsableId,
    LocalDateTime fechaRegistro,
    // Discrepancia por capacidad de tanque excedida (cruce con
    // asset_fuel_config.tanqueCapacidadGal) — calculada por AssetFuelCapacityService,
    // no por esta clase, para que quede centralizada en el backend y reutilizable
    // por cualquier consumidor de tanqueos (creación, edición, listados, reportes).
    Boolean capacidadExcedida
) {
    public static RefuelingRecordResponse from(RefuelingRecordsEntity entity, boolean capacidadExcedida) {
        return new RefuelingRecordResponse(
                entity.getId(),
                entity.getVehicleId(),
                entity.getMachineId(),
                entity.getLugar(),
                entity.getAreaCosto(),
                entity.getFuelTypeId(),
                entity.getCantidadGalones(),
                entity.getHorometroKm(),
                entity.getEsFull(),
                entity.getPrecioUnitario(),
                entity.getDescuento(),
                entity.getTotalIngresado(),
                entity.getTotalCalculado(),
                entity.getDiscrepanciaValor(),
                entity.getUrlFactura(),
                entity.getOrigen(),
                entity.getResponsableId(),
                entity.getFechaRegistro() != null ? entity.getFechaRegistro().toLocalDateTime() : null,
                capacidadExcedida
        );
    }
}
