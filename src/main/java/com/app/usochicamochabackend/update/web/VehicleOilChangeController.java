package com.app.usochicamochabackend.update.web;

import com.app.usochicamochabackend.update.application.dto.VehicleOilChangeHistoryDTO;
import com.app.usochicamochabackend.update.application.dto.VehicleOilChangeRequest;
import com.app.usochicamochabackend.update.application.service.VehicleOilChangeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicle/oil-change")
@RequiredArgsConstructor
@Tag(
                name = "Vehicle Oil Change",
                description = "Registro de cambios de aceite del vehículo en `vehicle_oil_changes`. Aplica a cualquier fila de `vehiculos` "
                                + "(incluye **motocicletas** si comparten el mismo mecanismo de mantenimiento). "
                                + "Roles: MECANIC o ADMIN para POST.")
public class VehicleOilChangeController {

    private final VehicleOilChangeService vehicleOilChangeService;

    @PostMapping
    @Operation(
                    summary = "Registrar cambio de aceite",
                    description = "Cuerpo `VehicleOilChangeRequest` (placa, km de cambio, intervalo, etc.). "
                                    + "Validaciones de km pueden alinearse con `GET /api/v1/vehicle-inspection/validar-kilometraje`.")
    public ResponseEntity<Void> registerChange(@RequestBody VehicleOilChangeRequest request) {
        vehicleOilChangeService.registerChange(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history/{placa}")
    @Operation(
                    summary = "Historial de cambios de aceite por placa",
                    description = "Devuelve todos los registros de cambio de aceite para un vehículo ordenados por fecha DESC.")
    public ResponseEntity<List<VehicleOilChangeHistoryDTO>> getHistory(@PathVariable String placa) {
        return ResponseEntity.ok(vehicleOilChangeService.getHistoryByPlaca(placa));
    }
}
