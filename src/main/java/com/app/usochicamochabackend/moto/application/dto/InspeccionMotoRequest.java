package com.app.usochicamochabackend.moto.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

@Schema(
        name = "InspeccionMotoRequest",
        description = "Inspección diaria simplificada de moto. Persiste cabecera, detalle mecánico y detalle de documentos. "
                        + "El usuario responsable se toma del JWT. Si `kilometrajeReportado` > 0 se actualiza el odómetro del vehículo.")
public record InspeccionMotoRequest(
        // Cabecera Principal y Detalles Vehiculo
        @Schema(description = "FK a `vehiculos.id_vehiculo` (debe ser tipo moto)", example = "4", requiredMode = RequiredMode.REQUIRED)
        Integer idVehiculo,
        @Schema(description = "Odómetro reportado en campo; actualiza `vehiculos.kilometraje_actual` si es positivo", example = "15200")
        Integer kilometrajeReportado,
        @Schema(description = "Estado general de la moto (texto libre o valor acordado, p. ej. EXCELENTE)", example = "BUENO")
        String estadoVehiculo,
        @Schema(description = "Observaciones finales del inspector") String observacionesFinales,

        // Detalle Documentos (valores de chequeo visual / estado)
        @Schema(description = "Estado declarado del SOAT", example = "Vigente")
        String checkSoat,
        @Schema(description = "Estado declarado de tecnomecánica", example = "Vigente")
        String checkTecno,
        @Schema(description = "Extintor u observación (ej. N/A si no aplica)", example = "N/A")
        String checkExtintor,

        // Detalle Mecánico Moto (Bueno / Regular / Malo)
        @Schema(description = "Nivel de aceite (Bueno / Regular / Malo)", example = "Bueno")
        String checkNivelAceite,
        @Schema(description = "Estado de llantas", example = "Bueno")
        String checkEstadoLlantas,
        @Schema(description = "Estado de luces", example = "Bueno")
        String checkEstadoLuces,

        // Ubicación (Migrado a General)
        @Schema(description = "Id en `cat_ubicaciones` donde se reporta la inspección", example = "2")
        Integer idUbicacion) {
}
