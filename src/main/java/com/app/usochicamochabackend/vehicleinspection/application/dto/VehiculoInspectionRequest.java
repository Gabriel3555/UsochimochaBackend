package com.app.usochicamochabackend.vehicleinspection.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de la inspección pre-operativa de vehículo enviada desde el móvil.
 * <p>
 * Los checks de documentación van a {@code insp_detalle_documentos}.
 * Si el cliente envía fechas / URLs opcionales, se fusionan en
 * {@code documentacion_y_elementos} (misma lógica que el alta desde web/admin).
 * Campos desconocidos (p. ej. marca informativa) se ignoran en JSON.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
        name = "VehiculoInspectionRequest",
        description = "Payload completo para registrar una inspección pre-operativa de vehículo (no moto simplificado). "
                        + "Identificación del vehículo por placa. El inspector se toma del JWT.")
public record VehiculoInspectionRequest(

        // ── inspeccion_pre_operativa ──────────────────────────────────────────
        @Schema(description = "Placa del vehículo (resolución a id interno en servidor)", example = "ABC123")
        String placaVehiculo,
        @Schema(description = "Odómetro reportado") Integer kilometrajeReportado,
        @Schema(description = "Si la ruta queda aprobada operativamente") Boolean aprobadoRuta,
        @Schema(description = "Observaciones finales") String observacionesFinales,

        // ── insp_detalle_mecanico ─────────────────────────────────────────────
        @Schema(description = "Nivel aceite") String nivelAceite,
        @Schema(description = "Nivel refrigerante") String nivelRefrigerante,
        @Schema(description = "Estado frenos") String nivelFrenos,
        @Schema(description = "Llantas") String estadoLlantas,
        @Schema(description = "Luces") String lucesGeneral,
        @Schema(description = "Estado visual") String estadoVisual,
        @Schema(description = "Limpieza general") String limpiezaGeneral,

        // ── insp_detalle_documentos ───────────────────────────────────────────
        @Schema(description = "Cheque visual SOAT: Vigente | Próximo a Vencer | Vencido") String checkSoat,
        @Schema(description = "Cheque tecnomecánica") String checkTecno,
        @Schema(description = "Cheque licencia") String checkLicencia,
        @Schema(description = "Cheque extintor") String checkExtintor,

        // ── documentacion_y_elementos (opcional, desde móvil) ─────────────────
        /** Fecha vencimiento SOAT (ISO yyyy-MM-dd o prefijo de ISO-8601). */
        @Schema(description = "Fecha vencimiento SOAT (ISO)", example = "2026-12-31")
        String fechaVencSoat,
        @Schema(description = "Fecha vencimiento tecnomecánica") String fechaVencTecno,
        @Schema(description = "Fecha vencimiento licencia") String fechaVencLicencia,
        /** Vigencia extintor en formato {@code yyyy-MM}. */
        @Schema(description = "Vigencia extintor año-mes", example = "2026-08")
        String vigenciaExtintor,
        @Schema(description = "URL o nombre de archivo imagen SOAT") String urlImagenSoat,
        @Schema(description = "URL o nombre de archivo tecno") String urlImagenTecno,
        @Schema(description = "URL o nombre de archivo licencia") String urlImagenLicencia,
        @Schema(description = "URL o nombre de archivo extintor") String urlImagenExtintor,

        // ── insp_detalle_elementos ────────────────────────────────────────────
        @Schema(description = "Botiquín") Boolean tieneBotiquin,
        @Schema(description = "Señalización") Boolean tieneSeñalizacion,
        @Schema(description = "Líneas de emergencia (mapeo legacy en BD)") Boolean tieneLineasEmergencia,
        @Schema(description = "Llanta repuesto") Boolean tieneLlantaRepuesto,
        @Schema(description = "Gato hidráulico") Boolean tieneGatoHidraulico,

        // ── insp_detalle_salud ────────────────────────────────────────────────
        @Schema(description = "Salud física") Boolean saludFisica,
        @Schema(description = "Salud mental") Boolean saludMental,
        @Schema(description = "Sin alcohol") Boolean sobrio,
        @Schema(description = "Medicamentos") Boolean medicamentos,
        @Schema(description = "Consciencia de responsabilidad") Boolean conscienteResponsabilidad,
        @Schema(description = "Condición para conducir") Boolean condicionParaConducir,
        @Schema(description = "Id ubicación en `cat_ubicaciones` donde se reporta", example = "2")
        Integer idUbicacion) {
}
