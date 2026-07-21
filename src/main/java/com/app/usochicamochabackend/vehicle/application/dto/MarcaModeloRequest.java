package com.app.usochicamochabackend.vehicle.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Alta de marca/modelo en `cat_marcas_modelos`.")
public record MarcaModeloRequest(
        @NotBlank(message = "La descripción de la marca es obligatoria")
        @Schema(description = "Nombre comercial visible", example = "Toyota", requiredMode = RequiredMode.REQUIRED)
        String descripcion
) {
}
