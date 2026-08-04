package com.app.usochicamochabackend.update.web;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.app.usochicamochabackend.update.application.dto.*;
import com.app.usochicamochabackend.update.application.port.*;
import com.app.usochicamochabackend.update.application.service.ExcelGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping({"/api/oil-changes", "/api/v1/oil-changes"})
@RequiredArgsConstructor
@Tag(name = "Oil Changes", description = "Operations related to motor and hydraulic oil changes")
public class OilChangeController {

    private final GetConsolidateHydraulicAndMotorOilAllMachinesUseCase getConsolidateHydraulicAndMotorOilAllMachines;
    private final PerformMotorOilChangeUseCase performMotorOilChange;
    private final PerformHydraulicChangeUseCase performHydraulicChange;
    private final ExcelGenerationService excelGenerationService;
    private final ManageMachineOilChangeHistoryUseCase manageMachineOilChangeHistoryUseCase;

    @GetMapping("/consolidated")
    @Operation(summary = "Get consolidated motor and hydraulic oil for all machines")
    public List<ConsolidateHydraulicAndMotorOilDTO> getAllConsolidated() {
        return getConsolidateHydraulicAndMotorOilAllMachines.getConsolidateHydraulicAndMotorOilAllMachines();
    }

    @GetMapping("/consolidated/excel")
    @Operation(summary = "Get consolidated motor and hydraulic oil for all machines in Excel format")
    public ResponseEntity<byte[]> getAllConsolidatedExcel() throws IOException {
        List<ConsolidateHydraulicAndMotorOilDTO> consolidatedData =
            getConsolidateHydraulicAndMotorOilAllMachines.getConsolidateHydraulicAndMotorOilAllMachines();

        byte[] excelBytes = excelGenerationService.generateConsolidatedMachinesExcel(consolidatedData);

        String filename = "consolidado-maquinas-" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")) + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(excelBytes.length);

        return ResponseEntity.ok()
            .headers(headers)
            .body(excelBytes);
    }

    @PostMapping("/motor")
    @Operation(summary = "Register a motor oil change")
    public PerformChangeMotorOilResponse performMotorOilChange(@RequestBody PerformChangeMotorOilRequest request) {
        return performMotorOilChange.performMotorOilChange(request);
    }

    @PostMapping("/hydraulic")
    @Operation(summary = "Register a hydraulic oil change")
    public PerformChangeHydraulicOilResponse performHydraulicOilChange(@RequestBody PerformChangeHydraulicOilRequest request) {
        return performHydraulicChange.performChangeHydraulicOil(request);
    }

    @GetMapping("/machine/{machineId}/history")
    @Operation(
            summary = "Historial de cambios de aceite de una máquina",
            description = "tipo debe ser MOTOR o HYDRAULIC. Devuelve todos los registros activos ordenados por fecha DESC.")
    public ResponseEntity<List<MachineOilChangeHistoryDTO>> getHistory(
            @PathVariable Long machineId, @RequestParam String tipo) {
        return ResponseEntity.ok(manageMachineOilChangeHistoryUseCase.obtenerHistorial(machineId, tipo));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Corregir un cambio de aceite de maquinaria ya registrado",
            description = "Actualiza cualquier registro del historial (no solo el más reciente), en caso de error de captura. El tipo (motor/hidráulico) del registro no cambia.")
    public ResponseEntity<Void> actualizar(@PathVariable Long id, @RequestBody PerformChangeMotorOilRequest request) {
        manageMachineOilChangeHistoryUseCase.actualizarCambioAceite(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar un cambio de aceite de maquinaria (soft-delete)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        manageMachineOilChangeHistoryUseCase.eliminarCambioAceite(id);
        return ResponseEntity.noContent().build();
    }
}