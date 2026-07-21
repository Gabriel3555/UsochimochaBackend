package com.app.usochicamochabackend.vehicleinspection.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(name = "DocumentoVehiculoVersionDTO", description = "Una versión almacenada de documentación del vehículo (historial).")
public record DocumentoVehiculoVersionDTO(
        @Schema(description = "PK documento") Integer idDocumento,
        @Schema(description = "Tipo en BD") String tipoDocumento,
        @Schema(description = "Fin de vigencia") LocalDate fechaVigencia,
        @Schema(description = "URL absoluta o ruta servible bajo /uploads") String urlArchivo,
        @Schema(description = "MIME, p. ej. image/jpeg o application/pdf") String contentType,
        @Schema(description = "Momento de registro") LocalDateTime subidoEn,
        @Schema(description = "Usuario que registró (login)") String subidoPor,
        @Schema(description = "Si es la versión operativa actual") Boolean vigente,
        @Schema(description = "Estado calculado por vigencia") String estadoCalculado
) {}
