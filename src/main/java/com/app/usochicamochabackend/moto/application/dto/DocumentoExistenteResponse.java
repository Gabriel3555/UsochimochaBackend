package com.app.usochicamochabackend.moto.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * Respuesta de pre-llenado para app: SOAT, REVISION_TECNO, LICENCIA.
 * <p>
 * El estado operativo se calcula en servidor a partir de la vigencia en `documentacion_y_elementos`
 * (no necesariamente desde la última fila de inspección).
 */
@Schema(
        name = "DocumentoExistenteResponse",
        description = "Documento de moto: tipo, vigencia, URL de imagen y estado calculado.")
public record DocumentoExistenteResponse(
        @Schema(description = "Id interno del documento maestro si aplica") Integer id,
        @Schema(description = "SOAT | REVISION_TECNO | LICENCIA", example = "SOAT")
        String tipoDocumento,
        @Schema(description = "Fin de vigencia del documento") LocalDate fechaVencimiento,
        @Schema(description = "Mes-año tipo YYYY-MM derivado de la vigencia", example = "2026-11")
        String mesyear,
        @Schema(description = "URL absoluta o ruta publicable de la imagen") String imagenUrl,
        @Schema(description = "Kilometraje actual del vehículo al momento de la consulta") Integer vehiculoKilometrajeActual,
        @Schema(description = "Vigente | Próximo a Vencer | Vencido | Sin Información", example = "Vigente")
        String estadoCheck) {
}
