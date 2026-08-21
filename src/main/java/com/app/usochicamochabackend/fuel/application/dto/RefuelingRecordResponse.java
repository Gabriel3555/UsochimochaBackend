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
    Boolean capacidadExcedida,
    Boolean cantidadFueraDeRango,
    Boolean precioFueraDeRango,
    Boolean fullInconsistente,
    BigDecimal cantidadReintegrada,
    BigDecimal capacidadConfiguradaGal,
    BigDecimal cantidadMaximaTipica,
    BigDecimal precioPromedioReciente,
    BigDecimal cantidadEsperadaLlenoGal,
    // Nombre descriptivo del activo (placa de vehículo/moto o nombre de máquina)
    String assetName,
    // Tipo de activo legible: "Vehículo", "Motocicleta", "Máquina"
    String assetType
) {
    public static RefuelingRecordResponse from(RefuelingRecordsEntity entity, boolean capacidadExcedida,
            boolean cantidadFueraDeRango, boolean precioFueraDeRango, boolean fullInconsistente,
            BigDecimal cantidadReintegrada, BigDecimal capacidadConfiguradaGal, BigDecimal cantidadMaximaTipica,
            BigDecimal precioPromedioReciente, BigDecimal cantidadEsperadaLlenoGal, String assetName, String assetType) {
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
                capacidadExcedida,
                cantidadFueraDeRango,
                precioFueraDeRango,
                fullInconsistente,
                cantidadReintegrada,
                capacidadConfiguradaGal,
                cantidadMaximaTipica,
                precioPromedioReciente,
                cantidadEsperadaLlenoGal,
                assetName,
                assetType
        );
    }
}
