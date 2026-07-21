package com.app.usochicamochabackend.moto.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Entrada del catálogo `cat_tipos_vehiculo` (listados y formularios).")
public record TipoVehiculoResponse(
        @Schema(description = "id_tipo_vehiculo", example = "1")
        Integer id,
        @Schema(description = "Nombre del tipo", example = "MOTOCICLETA")
        String nombreTipo,
        @Schema(description = "Si el tipo está habilitado para nuevas altas", example = "true")
        Boolean activo
) {
}
