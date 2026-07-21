package com.app.usochicamochabackend.moto.web;

import com.app.usochicamochabackend.moto.application.dto.TipoVehiculoResponse;
import com.app.usochicamochabackend.moto.application.port.FindAllTiposVehiculoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicle/type")
@RequiredArgsConstructor
@Tag(
                name = "Tipos de Vehículo",
                description = "Listado de solo lectura de `cat_tipos_vehiculo` como `TipoVehiculoResponse` (id, nombreTipo, activo). "
                                + "La administración CRUD del mismo catálogo vive en `/api/v1/catalog/tipo-vehiculo` con `CatalogDTO`.")
public class TipoVehiculoController {

    private final FindAllTiposVehiculoUseCase findAllTiposVehiculoUseCase;

    @GetMapping
    @Operation(
                    summary = "Listar tipos de vehículos",
                    description = "Equivalente semántico al GET de catálogo pero con otra forma de DTO; pensado para clientes que esperan `nombreTipo`.")
    @ApiResponse(responseCode = "200", description = "Lista retornada exitosamente")
    public ResponseEntity<List<TipoVehiculoResponse>> getAllTipos() {
        return ResponseEntity.ok(findAllTiposVehiculoUseCase.findAll());
    }
}
