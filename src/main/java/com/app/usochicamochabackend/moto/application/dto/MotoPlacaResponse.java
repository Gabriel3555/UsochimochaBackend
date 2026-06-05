package com.app.usochicamochabackend.moto.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Referencia mínima para listas de trabajo: id, placa, marca y ubicación base de moto activa.")
public record MotoPlacaResponse(
        @Schema(description = "`vehiculos.id_vehiculo`", example = "4") Integer id,
        @Schema(description = "Placa", example = "MOT001") String placa,
        @Schema(description = "Marca/modelo de la moto", example = "Yamaha YZF-R3") String marca,
        @Schema(description = "ID de la ubicación base de la moto") Integer idUbicacionBase,
        @Schema(description = "Nombre de la ubicación base de la moto") String ubicacionBase) {
}
