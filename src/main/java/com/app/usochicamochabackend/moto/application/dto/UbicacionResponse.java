package com.app.usochicamochabackend.moto.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ubicación operativa activa del catálogo.")
public record UbicacionResponse(
        @Schema(description = "`cat_ubicaciones.id_ubicacion`", example = "2") Integer id,
        @Schema(description = "Nombre visible", example = "Campo Málaga") String nombreUbicacion) {
}
