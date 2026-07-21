package com.app.usochicamochabackend.vehicleinspection.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta del endpoint de validación de kilometraje.
 *
 * @param alerta  {@code true} si el kilometraje ingresado es menor al
 *                registrado;
 *                {@code false} si es correcto.
 * @param mensaje Descripción legible del resultado de la validación.
 */
@Schema(description = "Resultado de comparar km ingresado vs último km registrado del vehículo por placa.")
public record KilometrajeValidacionResponse(
        @Schema(description = "true si el valor ingresado es menor que el registrado en BD (posible error o rollback de odómetro)")
        boolean alerta,
        @Schema(description = "Explicación para mostrar al usuario")
        String mensaje) {
}
