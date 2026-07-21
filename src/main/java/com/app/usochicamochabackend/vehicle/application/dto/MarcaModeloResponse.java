package com.app.usochicamochabackend.vehicle.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Marca de vehículo del catálogo (respuesta).")
public record MarcaModeloResponse(
        @Schema(description = "PK `id_marca`", example = "1")
        Integer idMarca,
        @Schema(description = "Nombre", example = "Chevrolet")
        String descripcion
) {
}
