package com.app.usochicamochabackend.vehicle.application.dto;

import com.app.usochicamochabackend.common.text.InputTextNormalizer;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

@Schema(
        name = "VehicleRequest",
        description = "Alta/edición de un registro en `vehiculos`. En `POST /api/v1/moto` el servidor ignora `idTipoVehiculo` del cliente y fuerza MOTOCICLETA.")
public record VehicleRequest(
        @NotBlank(message = "La placa es obligatoria")
        @Schema(description = "Placa única en toda la tabla vehículos", example = "ABC123", requiredMode = RequiredMode.REQUIRED)
        String placa,

        @NotNull(message = "La marca es obligatoria")
        @Schema(description = "FK a `cat_marcas_modelos.id_marca`", example = "1", requiredMode = RequiredMode.REQUIRED)
        Integer idMarca,

        @NotNull(message = "El tipo de vehículo es obligatorio")
        @Schema(
                description = "FK a `cat_tipos_vehiculo`. En CRUD moto se sobrescribe con el id del tipo MOTOCICLETA",
                example = "2",
                requiredMode = RequiredMode.REQUIRED)
        Integer idTipoVehiculo,

        @NotNull(message = "El kilometraje es obligatorio")
        @Min(value = 0, message = "El kilometraje no puede ser negativo")
        @Schema(description = "Kilometraje odométrico actual", example = "128500", requiredMode = RequiredMode.REQUIRED)
        Integer kilometrajeActual,

        @Schema(description = "Etiqueta de área organizacional (texto libre o valor de catálogo de áreas según cliente)", example = "distrito")
        String belongsTo,

        /** Catálogo {@code cat_ubicaciones}; opcional (p. ej. Unidad Pantano). */
        @Schema(description = "FK opcional a `cat_ubicaciones` (ubicación base / estación)", example = "2")
        Integer idUbicacionBase,

        @Schema(description = "Si el vehículo está activo en inventario (en alta vehículo el backend puede forjar `true`)", example = "true")
        Boolean activo,

        @DecimalMin(value = "0.1", message = "La capacidad del tanque debe ser mayor a 0")
        @Schema(description = "Capacidad real del tanque en galones (solo ADMIN). Limita la cantidad máxima que puede registrarse por carga.", example = "18.5")
        BigDecimal fuelTankCapacityGallons,

        @Schema(description = "Eficiencia de fábrica — valor numérico (referencia del fabricante, opcional).", example = "42.5")
        BigDecimal factoryEfficiencyKmPerGallon,

        @Schema(description = "Unidad de la eficiencia de fábrica: KM_PER_GALLON (combustible líquido) o KM_PER_CUBIC_METER (gas natural).", example = "KM_PER_GALLON")
        String factoryEfficiencyUnit
) {
    /** Placa en mayúsculas sin espacios; {@code belongsTo} en minúsculas; resto sin cambiar. */
    public VehicleRequest normalized() {
        return new VehicleRequest(
                InputTextNormalizer.normalizePlaca(placa),
                idMarca,
                idTipoVehiculo,
                kilometrajeActual,
                belongsTo != null ? belongsTo.toLowerCase().trim() : null,
                idUbicacionBase,
                activo,
                fuelTankCapacityGallons,
                factoryEfficiencyKmPerGallon,
                factoryEfficiencyUnit);
    }
}
