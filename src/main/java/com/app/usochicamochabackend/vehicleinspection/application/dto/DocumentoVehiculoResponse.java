package com.app.usochicamochabackend.vehicleinspection.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta del GET /api/v1/vehicle-inspection/documentos/{idVehiculo}
 * Retorna la fecha de vencimiento más reciente de cada tipo de documento
 * y su estado calculado automáticamente (Vigente / Próximo a Vencer / Vencido).
 */
@Schema(
        name = "DocumentoVehiculoResponse",
        description = "Snapshot de vigencias e imágenes por tipo de documento para un vehículo.")
public record DocumentoVehiculoResponse(
        @Schema(description = "Id del vehículo") Integer idVehiculo,

        // SOAT
        @Schema(description = "Fecha fin vigencia SOAT (YYYY-MM-DD)", example = "2026-11-30")
        String fechaVencSoat,
        @Schema(description = "Vigente | Próximo a Vencer | Vencido") String estadoSoat,
        @Schema(description = "Imagen asociada al SOAT") String urlImagenSoat,

        // Tecnomecánica
        @Schema(description = "Fecha fin tecnomecánica") String fechaVencTecno,
        @Schema(description = "Estado tecnomecánica") String estadoTecno,
        @Schema(description = "Imagen tecno") String urlImagenTecno,

        // Tarjeta de propiedad (sin fecha de vencimiento)
        @Schema(description = "Imagen tarjeta de propiedad") String urlImagenTarjetaPropiedad,

        // Extintor
        @Schema(description = "Fecha fin vigencia extintor (según almacenamiento)") String fechaVencExtintor,
        @Schema(description = "Estado extintor") String estadoExtintor,
        @Schema(description = "Imagen extintor") String urlImagenExtintor) {
}
