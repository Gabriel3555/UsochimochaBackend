package com.app.usochicamochabackend.catalog.web;

import com.app.usochicamochabackend.catalog.application.dto.CatalogDTO;
import com.app.usochicamochabackend.catalog.application.service.AreaService;
import com.app.usochicamochabackend.catalog.application.service.TipoVehiculoService;
import com.app.usochicamochabackend.catalog.application.service.UbicacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
@Tag(
                name = "Catalog",
                description = "CRUD de catálogos usados por vehículos y motos: **áreas** organizacionales, **ubicaciones** operativas "
                                + "(p. ej. id de base en inventario) y **tipos de vehículo** (`cat_tipos_vehiculo`). "
                                + "Todos comparten el mismo esquema JSON `CatalogDTO`. Requiere rol **ADMIN** para POST/PUT/DELETE.")
public class CatalogController {

    private final AreaService areaService;
    private final UbicacionService ubicacionService;
    private final TipoVehiculoService tipoVehiculoService;

    // --- Areas ---
    @GetMapping("/area")
    @Operation(summary = "Listar áreas", description = "Catálogo `cat_areas`: id, nombre y activo.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Lista de áreas"))
    public ResponseEntity<List<CatalogDTO>> getAreas() {
        return ResponseEntity.ok(areaService.findAll());
    }

    @PostMapping("/area")
    @Operation(
                    summary = "Crear área",
                    description = "Body: `name` obligatorio; `active` en alta se ignora (se crea activa). Respuesta con id asignado.")
    @ApiResponses({
                    @ApiResponse(responseCode = "200", description = "Área creada (cuerpo CatalogDTO)"),
                    @ApiResponse(responseCode = "403", description = "Sin permisos ADMIN")
    })
    public ResponseEntity<CatalogDTO> createArea(@RequestBody CatalogDTO dto) {
        return ResponseEntity.ok(areaService.create(dto));
    }

    @PutMapping("/area/{id}")
    @Operation(summary = "Actualizar área", description = "Actualiza nombre y flag activo.")
    @ApiResponses({
                    @ApiResponse(responseCode = "200", description = "Actualizada"),
                    @ApiResponse(responseCode = "404", description = "Id no existe")
    })
    public ResponseEntity<CatalogDTO> updateArea(@PathVariable Integer id, @RequestBody CatalogDTO dto) {
        return ResponseEntity.ok(areaService.update(id, dto));
    }

    @DeleteMapping("/area/{id}")
    @Operation(summary = "Eliminar área", description = "Elimina la fila por id.")
    @ApiResponses({
                    @ApiResponse(responseCode = "204", description = "Eliminada"),
                    @ApiResponse(responseCode = "403", description = "Sin permisos ADMIN")
    })
    public ResponseEntity<Void> deleteArea(@PathVariable Integer id) {
        areaService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // --- Ubicaciones ---
    @GetMapping("/ubicacion")
    @Operation(
                    summary = "Listar ubicaciones",
                    description = "Catálogo `cat_ubicaciones` (sedes, unidades, talleres). Usado en `VehicleRequest.idUbicacionBase` e inspecciones.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Lista de ubicaciones"))
    public ResponseEntity<List<CatalogDTO>> getUbicaciones() {
        return ResponseEntity.ok(ubicacionService.findAll());
    }

    @PostMapping("/ubicacion")
    @Operation(
                    summary = "Crear ubicación",
                    description = "Misma semántica que área: en alta solo se requiere `name`; activo queda en true en servidor.")
    @ApiResponses({
                    @ApiResponse(responseCode = "200", description = "Ubicación creada"),
                    @ApiResponse(responseCode = "403", description = "Sin permisos ADMIN")
    })
    public ResponseEntity<CatalogDTO> createUbicacion(@RequestBody CatalogDTO dto) {
        return ResponseEntity.ok(ubicacionService.create(dto));
    }

    @PutMapping("/ubicacion/{id}")
    @Operation(summary = "Actualizar ubicación", description = "Actualiza nombre y activo.")
    @ApiResponses({
                    @ApiResponse(responseCode = "200", description = "Actualizada"),
                    @ApiResponse(responseCode = "404", description = "Id no existe")
    })
    public ResponseEntity<CatalogDTO> updateUbicacion(@PathVariable Integer id, @RequestBody CatalogDTO dto) {
        return ResponseEntity.ok(ubicacionService.update(id, dto));
    }

    @DeleteMapping("/ubicacion/{id}")
    @Operation(summary = "Eliminar ubicación", description = "Elimina por id.")
    @ApiResponses({
                    @ApiResponse(responseCode = "204", description = "Eliminada"),
                    @ApiResponse(responseCode = "403", description = "Sin permisos ADMIN")
    })
    public ResponseEntity<Void> deleteUbicacion(@PathVariable Integer id) {
        ubicacionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // --- Tipos de Vehiculo ---
    @GetMapping("/tipo-vehiculo")
    @Operation(
                    summary = "Listar tipos de vehículo",
                    description = "Catálogo `cat_tipos_vehiculo` (MOTOCICLETA, AUTOMOVIL, …). Coherente con `VehicleRequest.idTipoVehiculo`.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Lista de tipos"))
    public ResponseEntity<List<CatalogDTO>> getTiposVehiculo() {
        return ResponseEntity.ok(tipoVehiculoService.findAll());
    }

    @PostMapping("/tipo-vehiculo")
    @Operation(
                    summary = "Crear tipo de vehículo",
                    description = "Alta de un nombre de tipo; debe ser coherente con constantes de negocio (ej. MOTOCICLETA). Alta fuerza activo=true.")
    @ApiResponses({
                    @ApiResponse(responseCode = "200", description = "Tipo creado"),
                    @ApiResponse(responseCode = "403", description = "Sin permisos ADMIN")
    })
    public ResponseEntity<CatalogDTO> createTipoVehiculo(@RequestBody CatalogDTO dto) {
        return ResponseEntity.ok(tipoVehiculoService.create(dto));
    }

    @PutMapping("/tipo-vehiculo/{id}")
    @Operation(summary = "Actualizar tipo de vehículo", description = "Actualiza nombre y activo.")
    @ApiResponses({
                    @ApiResponse(responseCode = "200", description = "Actualizado"),
                    @ApiResponse(responseCode = "404", description = "Id no existe")
    })
    public ResponseEntity<CatalogDTO> updateTipoVehiculo(@PathVariable Integer id, @RequestBody CatalogDTO dto) {
        return ResponseEntity.ok(tipoVehiculoService.update(id, dto));
    }

    @DeleteMapping("/tipo-vehiculo/{id}")
    @Operation(summary = "Eliminar tipo de vehículo", description = "Elimina por id (validar que no rompa FK en vehículos).")
    @ApiResponses({
                    @ApiResponse(responseCode = "204", description = "Eliminado"),
                    @ApiResponse(responseCode = "403", description = "Sin permisos ADMIN")
    })
    public ResponseEntity<Void> deleteTipoVehiculo(@PathVariable Integer id) {
        tipoVehiculoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
