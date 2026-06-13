package com.app.usochicamochabackend.vehicle.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(
        name = "VehicleMonitoringDTO",
        description = "Fila del tablero de monitoreo vehicular (sin motos): documentos, aceite y último reporte.")
public record VehicleMonitoringDTO(
        @Schema(description = "Placa del vehículo") String placa,
        @Schema(description = "Área / distrito o etiqueta organizacional") String area,
        @Schema(description = "Kilometraje actual en BD") Integer kmActual,
        @Schema(description = "Fecha/hora del último reporte de inspección u odómetro asociado") LocalDateTime fechaUltimoReporte,
        @Schema(description = "Días transcurridos desde el último reporte (si aplica)") Long diasUltimoReporte,
        @Schema(description = "Estado calculado del SOAT") DocumentStatus soat,
        @Schema(description = "Estado calculado de tecnomecánica") DocumentStatus tecno,
        @Schema(description = "Estado del mantenimiento de aceite") OilStatus maintenance
) {
    @Schema(
            name = "VehicleMonitoringDocumentStatus",
            description = "Vigencia y estado operativo de un documento (vehículos).")
    public record DocumentStatus(
            @Schema(description = "Fecha fin de vigencia") LocalDate fechaVencimiento,
            @Schema(description = "Días hasta vencimiento (negativo si ya venció)") Long diasRestantes,
            @Schema(description = "Vigente | Próximo a Vencer | Vencido") String estado
    ) {}

    @Schema(
            name = "VehicleMonitoringOilStatus",
            description = "Resumen del último cambio de aceite e intervalo.")
    public record OilStatus(
            @Schema(description = "Marca del aceite") String brandName,
            @Schema(description = "Cantidad de aceite (litros)") Double quantity,
            @Schema(description = "Intervalo de cambio en km") Integer intervalKm,
            @Schema(description = "Fecha del último cambio registrado") LocalDate fechaUltimoCambio,
            @Schema(description = "Km en que se registró el cambio") Integer kmCambio,
            @Schema(description = "Km objetivo del próximo cambio") Integer kmProximoCambio,
            @Schema(description = "Km restantes hasta el próximo cambio") Integer kmParaProximo,
            @Schema(description = "Días desde último cambio") Long diasDesdeUltimoCambio,
            @Schema(description = "Si se cambió filtro de aire") Boolean filtroAire,
            @Schema(description = "Estado operativo del aceite (p. ej. OK, atención)") String estado,
            @Schema(description = "Porcentaje de uso del intervalo (0-100+)") Double percentageUsed,
            @Schema(description = "Color de alerta: GREEN, BLUE, YELLOW, RED") String alertColor,
            @Schema(description = "Mensaje de alerta para operario") String alertMessage
    ) {}
}
