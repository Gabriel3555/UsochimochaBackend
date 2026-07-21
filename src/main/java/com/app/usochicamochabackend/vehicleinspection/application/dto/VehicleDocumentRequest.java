package com.app.usochicamochabackend.vehicleinspection.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(
        name = "VehicleDocumentRequest",
        description = "Alta o actualización de vigencia de un documento en `documentacion_y_elementos`. "
                        + "Tipos habituales: SOAT, TECNOMECANICA, LICENCIA DE CONDUCCION, EXTINTOR.")
public record VehicleDocumentRequest(
        @Schema(description = "FK `vehiculos.id_vehiculo`", example = "1")
        Integer idVehiculo,
        @Schema(description = "Código de tipo de documento alineado con BD", example = "SOAT")
        String tipoDocumento,
        @Schema(description = "Fin de vigencia")
        LocalDate fechaVencimiento,
        @Schema(description = "URL absoluta o ruta bajo `/uploads/documents/` según política de despliegue")
        String imagenUrl,
        @Schema(description = "Tipo MIME del archivo (opcional)", example = "application/pdf")
        String contentType
) {}
