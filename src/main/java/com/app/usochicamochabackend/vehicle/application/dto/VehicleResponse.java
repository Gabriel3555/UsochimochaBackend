package com.app.usochicamochabackend.vehicle.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(
        name = "VehicleResponse",
        description = "Vista de vehículo con marca y tipo resueltos a texto. Usado en `/api/v1/vehicle` y CRUD `/api/v1/moto`.")
public record VehicleResponse(
        @Schema(description = "PK `vehiculos.id_vehiculo`", example = "15")
        Integer id,
        @Schema(description = "Placa", example = "JKL456")
        String placa,
        @Schema(description = "Descripción de marca (`cat_marcas_modelos`)", example = "Toyota")
        String marca,
        @Schema(description = "FK `cat_marcas_modelos.id_marca`", example = "3")
        Integer idMarca,
        @Schema(description = "FK `cat_tipos_vehiculo.id_tipo_vehiculo`", example = "1")
        Integer idTipoVehiculo,
        @Schema(description = "Nombre del tipo (`cat_tipos_vehiculo`)", example = "AUTOMOVIL")
        String tipoVehiculo,
        @Schema(description = "Kilometraje actual registrado", example = "89000")
        Integer kilometrajeActual,
        @Schema(description = "Área / pertenencia organizacional", example = "Operaciones Chicamocha")
        String belongsTo,
        @Schema(description = "Id de ubicación base (`cat_ubicaciones`)", example = "2")
        Integer idUbicacionBase,
        @Schema(description = "Nombre legible de la ubicación base", example = "Campo Málaga")
        String ubicacionBase,

        @Schema(description = "Si el vehículo está activo en inventario", example = "true")
        Boolean activo,

        @Schema(description = "Capacidad del tanque en galones (null si no está configurada)", example = "18.5")
        BigDecimal fuelTankCapacityGallons,

        @Schema(description = "Eficiencia de fábrica — valor numérico (null si no se ingresó)", example = "42.5")
        BigDecimal factoryEfficiencyKmPerGallon,

        @Schema(description = "Unidad: KM_PER_GALLON | KM_PER_CUBIC_METER", example = "KM_PER_GALLON")
        String factoryEfficiencyUnit
) {
}
