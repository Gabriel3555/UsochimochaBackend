package com.app.usochicamochabackend.maintenance.web;

import com.app.usochicamochabackend.maintenance.application.dto.MaintenanceRequest;
import com.app.usochicamochabackend.maintenance.application.dto.MaintenanceResponse;
import com.app.usochicamochabackend.maintenance.application.port.MaintenanceUseCase;
import com.app.usochicamochabackend.update.application.service.ExcelGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/maintenance")
@RequiredArgsConstructor
@Tag(name = "Maintenance", description = "Gestión de mantenimientos preventivos y correctivos")
public class MaintenanceController {

    private final MaintenanceUseCase maintenanceUseCase;
    private final ExcelGenerationService excelGenerationService;

    @PostMapping
    @Operation(summary = "Registrar un mantenimiento")
    public ResponseEntity<Void> registerMaintenance(@RequestBody MaintenanceRequest request) {
        maintenanceUseCase.registerMaintenance(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/motos")
    @Operation(summary = "Obtener historial de mantenimiento de motos")
    public ResponseEntity<List<MaintenanceResponse>> getMotosMaintenance() {
        return ResponseEntity.ok(maintenanceUseCase.getMotosMaintenance());
    }

    @GetMapping("/vehicles")
    @Operation(summary = "Obtener historial de mantenimiento de vehículos")
    public ResponseEntity<List<MaintenanceResponse>> getVehiclesMaintenance() {
        return ResponseEntity.ok(maintenanceUseCase.getVehiclesMaintenance());
    }

    @GetMapping("/motos/export")
    @Operation(summary = "Exportar mantenimiento de motos a Excel")
    public ResponseEntity<byte[]> exportMotosMaintenance() throws IOException {
        byte[] excelData = excelGenerationService.generateMaintenanceExcel(maintenanceUseCase.getMotosMaintenance());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "mantenimiento_motos.xlsx");
        return ResponseEntity.ok().headers(headers).body(excelData);
    }

    @GetMapping("/vehicles/export")
    @Operation(summary = "Exportar mantenimiento de vehículos a Excel")
    public ResponseEntity<byte[]> exportVehiclesMaintenance() throws IOException {
        byte[] excelData = excelGenerationService.generateMaintenanceExcel(maintenanceUseCase.getVehiclesMaintenance());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "mantenimiento_vehiculos.xlsx");
        return ResponseEntity.ok().headers(headers).body(excelData);
    }
}
