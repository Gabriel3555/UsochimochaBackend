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
    Boolean capacidadExcedida,
    // Cantidad fuera de un rango razonable para el tipo de activo (ej. 300 gal en
    // una moto) — AssetFuelCapacityService.cantidadFueraDeRangoTipico, aplica aunque
    // el activo no tenga tanqueCapacidadGal configurado.
    Boolean cantidadFueraDeRango,
    // Precio unitario fuera de rango vs el promedio reciente del mismo combustible
    // en BOMBA — FuelPriceAnomalyService, null cuando no aplica (ALMACEN o sin
    // precioUnitario).
    Boolean precioFueraDeRango,
    // Suma de fuel_reintegrations para este tanqueo (ZERO si no hay ninguno, nunca
    // null — simplifica el cálculo de saldo disponible = cantidadGalones -
    // cantidadReintegrada en el frontend). Antes solo lo calculaba el endpoint viejo
    // GET /fuel/distribucion (FuelDistributionService.mapFila); se replica el mismo
    // cálculo acá para que Tanqueo y Distribución / Historial de tanqueos también
    // lo muestren.
    BigDecimal cantidadReintegrada
) {
    public static RefuelingRecordResponse from(RefuelingRecordsEntity entity, boolean capacidadExcedida,
            boolean cantidadFueraDeRango, boolean precioFueraDeRango, BigDecimal cantidadReintegrada) {
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
                cantidadReintegrada
        );
    }
}
