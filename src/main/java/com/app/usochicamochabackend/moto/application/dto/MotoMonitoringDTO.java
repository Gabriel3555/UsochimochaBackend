package com.app.usochicamochabackend.moto.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(
        name = "MotoMonitoringDTO",
        description = "Fila del tablero de monitoreo de motocicletas: documentos, aceite, ubicación y última inspección.")
public record MotoMonitoringDTO(
        @Schema(description = "Departamento / pertenencia (`belongs_to` o similar)") String departamento,
        /** Ubicación por defecto del vehículo (catálogo {@code id_ubicacion_base}). */
        @Schema(description = "Ubicación base del inventario (`cat_ubicaciones`)") String ubicacionBase,
        /**
         * Unidad/sitio de la última inspección (nombre de {@code cat_ubicaciones} vía {@code insp_pre_operativa.id_ubicacion}).
         * El nombre del campo en JSON se mantiene por compatibilidad con clientes existentes.
         */
        @Schema(description = "Nombre de la estación donde se reportó la última inspección (p. ej. móvil)") String responsable,
        @Schema(description = "Placa") String placa,
        @Schema(description = "Kilometraje actual") Integer kmActual,
        @Schema(description = "Estado del SOAT") DocumentStatus soat,
        @Schema(description = "Estado de tecnomecánica") DocumentStatus tecno,
        @Schema(description = "Estado del aceite / intervalo") OilStatus oil,
        @Schema(description = "Estado general declarado en última inspección") String estadoMoto,
        @Schema(description = "Fecha/hora del último reporte de inspección") LocalDateTime fechaUltimoReporte,
        @Schema(description = "Días desde el último reporte") Long diasUltimoReporte,
        @Schema(description = "Texto de novedades u observaciones finales recientes") String novedadActual
) {
    @Schema(
            name = "MotoMonitoringDocumentStatus",
            description = "Vigencia y estado de documento para motos en monitoreo.")
    public record DocumentStatus(
            @Schema(description = "Fin de vigencia") LocalDate fechaVencimiento,
            @Schema(description = "Días restantes de vigencia") Long diasRestantes,
            @Schema(description = "Texto de estado (Vigente, etc.)") String estado
    ) {}

    @Schema(
            name = "MotoMonitoringOilStatus",
            description = "Aceite: último cambio y proyección al siguiente.")
    public record OilStatus(
            @Schema(description = "Marca del aceite") String brandName,
            @Schema(description = "Cantidad de aceite (litros)") Double quantity,
            @Schema(description = "Intervalo de cambio en km") Integer intervalKm,
            @Schema(description = "Fecha último cambio de aceite") LocalDate fechaUltimoCambio,
            @Schema(description = "Km al momento del cambio") Integer kmCambio,
            @Schema(description = "Km objetivo próximo cambio") Integer kmProximoCambio,
            @Schema(description = "Km restantes hasta el próximo servicio") Integer kmParaProximo,
            @Schema(description = "Días desde último cambio") Long diasDesdeUltimoCambio,
            @Schema(description = "Si se cambió filtro de aire") Boolean filtroAire,
            @Schema(description = "Estado OK / atención según reglas de negocio") String estado,
            @Schema(description = "Porcentaje de uso del intervalo (0-100+)") Double percentageUsed,
            @Schema(description = "Color de alerta: GREEN, BLUE, YELLOW, RED") String alertColor,
            @Schema(description = "Mensaje de alerta para operario") String alertMessage
    ) {}
}
