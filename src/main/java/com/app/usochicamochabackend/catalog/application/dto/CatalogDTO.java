package com.app.usochicamochabackend.catalog.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * DTO genérico para tres catálogos bajo {@code /api/v1/catalog/*}: el campo {@code name} se persiste como
 * {@code nombre_area}, {@code nombre_ubicacion} o {@code nombre_tipo} según el subrecurso.
 * <p>
 * En <strong>POST</strong> (alta) el servidor ignora {@code id} y fija {@code active=true} al crear;
 * el cliente puede omitir {@code id} y {@code active}. En <strong>PUT</strong> se envía el nombre y
 * el estado deseado; el id de la ruta tiene prioridad.
 */
@Schema(
        name = "CatalogDTO",
        description = "Forma común de áreas, ubicaciones y tipos de vehículo. "
                        + "JSON usa siempre `name` y `active` (camelCase), no `nombre`/`activo`.")
public record CatalogDTO(
        @Schema(
                        description = "Identificador numérico. Ausente o null en altas; presente en respuestas y listados.",
                        example = "1")
        Integer id,
        @Schema(
                        description = "Texto visible: nombre de área, nombre de ubicación o nombre del tipo (ej. MOTOCICLETA).",
                        example = "Operaciones Chicamocha",
                        requiredMode = RequiredMode.REQUIRED)
        String name,
        @Schema(
                        description = "Si el ítem está habilitado. En POST el backend fuerza `true` al crear; en PUT se respeta el valor enviado.",
                        example = "true")
        Boolean active) {
}
