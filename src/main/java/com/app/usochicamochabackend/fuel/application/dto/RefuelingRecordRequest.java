package com.app.usochicamochabackend.fuel.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefuelingRecordRequest(
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
    String origen,
    // Fecha real del tanqueo (opcional): permite registrar/editar un tanqueo con
    // fecha distinta a "ahora" para no perder el histórico cuando se registra
    // tarde. Si viene null, registrar() usa la fecha actual y actualizar()
    // conserva la fecha que ya tenía el registro.
    LocalDateTime fecha
) {
    public RefuelingRecordRequest(Integer vehicleId, Long machineId, String lugar, String areaCosto, Long fuelTypeId,
            BigDecimal cantidadGalones, BigDecimal horometroKm, Boolean esFull, BigDecimal precioUnitario,
            BigDecimal descuento, BigDecimal totalIngresado, String origen) {
        this(vehicleId, machineId, lugar, areaCosto, fuelTypeId, cantidadGalones, horometroKm, esFull,
                precioUnitario, descuento, totalIngresado, origen, null);
    }
}
