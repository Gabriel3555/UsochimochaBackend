package com.app.usochicamochabackend.vehicle.web;

import com.app.usochicamochabackend.vehicle.application.dto.MarcaModeloRequest;
import com.app.usochicamochabackend.vehicle.application.dto.MarcaModeloResponse;
import com.app.usochicamochabackend.vehicle.application.port.MarcaModeloUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/brand/vehicle")
@RequiredArgsConstructor
@Tag(
                name = "Marcas de Vehículos",
                description = "CRUD de `cat_marcas_modelos` (descripción de marca). Referenciado por `VehicleRequest.idMarca`. "
                                + "Mismo catálogo para vehículos y motos. Requiere rol **ADMIN** para POST/PUT/DELETE.")
public class MarcaModeloController {

    private final MarcaModeloUseCase marcaModeloUseCase;

    @GetMapping
    @Operation(summary = "Listar marcas de vehículos", description = "Retorna todas las marcas de vehículos existentes")
    @ApiResponse(responseCode = "200", description = "Lista retornada exitosamente")
    public ResponseEntity<List<MarcaModeloResponse>> getAllMarcas() {
        return ResponseEntity.ok(marcaModeloUseCase.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener marca por ID", description = "Retorna una marca específica")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Marca encontrada"),
            @ApiResponse(responseCode = "404", description = "Marca no encontrada")
    })
    public ResponseEntity<MarcaModeloResponse> getMarcaById(@PathVariable Integer id) {
        return ResponseEntity.ok(marcaModeloUseCase.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crear marca", description = "Crea una nueva marca de vehículo")
    @ApiResponse(responseCode = "201", description = "Marca creada exitosamente")
    public ResponseEntity<MarcaModeloResponse> createMarca(@Valid @RequestBody MarcaModeloRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(marcaModeloUseCase.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar marca", description = "Actualiza la descripción de una marca existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Marca actualizada"),
            @ApiResponse(responseCode = "404", description = "Marca no encontrada")
    })
    public ResponseEntity<MarcaModeloResponse> updateMarca(@PathVariable Integer id, @Valid @RequestBody MarcaModeloRequest request) {
        return ResponseEntity.ok(marcaModeloUseCase.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar marca", description = "Elimina una marca existente")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Marca eliminada"),
            @ApiResponse(responseCode = "404", description = "Marca no encontrada")
    })
    public ResponseEntity<Void> deleteMarca(@PathVariable Integer id) {
        marcaModeloUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
