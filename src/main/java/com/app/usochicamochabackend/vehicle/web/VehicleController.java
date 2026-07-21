package com.app.usochicamochabackend.vehicle.web;

import com.app.usochicamochabackend.vehicle.application.dto.VehicleRequest;
import com.app.usochicamochabackend.vehicle.application.dto.VehicleResponse;
import com.app.usochicamochabackend.vehicle.application.port.VehicleUseCase;
import com.app.usochicamochabackend.vehicle.application.service.VehicleService;
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
@RequestMapping("/api/v1/vehicle")
@RequiredArgsConstructor
@Tag(
                name = "Vehicle",
                description = "Administración de vehículos (tabla `vehiculos`). El listado devuelve **todos** los registros activos; "
                                + "las motocicletas pueden aparecer aquí y también en el recurso `/api/v1/moto`. "
                                + "Campos típicos del cuerpo: placa, idMarca, idTipoVehiculo (`cat_tipos_vehiculo`), kilometrajeActual, belongsTo, idUbicacionBase, activo.")
public class VehicleController {

    private final VehicleUseCase vehicleUseCase;
    private final VehicleService vehicleService;

    @GetMapping
    @Operation(
                    summary = "Listar vehículos activos",
                    description = "Retorna todos los vehículos con `activo=true` (marca, tipo, km, área, ubicación base si aplica). "
                                    + "No filtra por tipo; puede incluir motocicletas si están en la misma tabla.")
    public ResponseEntity<List<VehicleResponse>> getAllVehicles() {
        return ResponseEntity.ok(vehicleUseCase.findAllVehicles());
    }

    @GetMapping("/{placa}")
    @Operation(
                    summary = "Obtener vehículo por placa",
                    description = "Detalle por placa (404 si no existe o no está activo). La placa en la URL debe coincidir con el registro.")
    public ResponseEntity<VehicleResponse> getVehicleByPlaca(@PathVariable String placa) {
        return ResponseEntity.ok(vehicleUseCase.findByPlaca(placa));
    }

    @PostMapping
    @Operation(
                    summary = "Crear vehículo",
                    description = "Alta en `vehiculos`. Placa única. Requiere JWT con rol **ADMIN**. "
                                    + "Al crear, el registro queda activo por defecto.")
    @ApiResponse(responseCode = "201", description = "Vehículo creado exitosamente")
    public ResponseEntity<VehicleResponse> createVehicle(@Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleUseCase.createVehicle(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar vehículo", description = "Actualiza la información de un vehículo existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehículo actualizado"),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado")
    })
    public ResponseEntity<VehicleResponse> updateVehicle(@PathVariable Integer id, @Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.ok(vehicleUseCase.updateVehicle(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar vehículo", description = "Elimina un vehículo.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vehículo eliminado"),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado")
    })
    public ResponseEntity<Void> deleteVehicle(@PathVariable Integer id) {
        vehicleUseCase.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    @Operation(
        summary = "Restaurar vehículo soft-deleted",
        description = "Reactiva un vehículo que fue eliminado (soft-delete). " +
            "Cambia status de false a true y registra en auditoría quién lo restauró.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vehículo restaurado"),
        @ApiResponse(responseCode = "404", description = "Vehículo eliminado no encontrado")
    })
    public ResponseEntity<VehicleResponse> restoreVehicle(@PathVariable Integer id) {
        VehicleResponse restored = vehicleService.restoreVehicle(id);
        return ResponseEntity.ok(restored);
    }
}
