package com.app.usochicamochabackend.fuel.web;

import com.app.usochicamochabackend.fuel.application.dto.FuelDashboardResponse;
import com.app.usochicamochabackend.fuel.application.dto.FuelLogRequest;
import com.app.usochicamochabackend.fuel.application.dto.FuelLogResponse;
import com.app.usochicamochabackend.fuel.application.dto.FuelMonthlyStatsResponse;
import com.app.usochicamochabackend.fuel.application.port.CreateFuelLogUseCase;
import com.app.usochicamochabackend.fuel.application.port.GetFuelDashboardUseCase;
import com.app.usochicamochabackend.fuel.application.port.GetFuelHistoryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/fuel")
@RequiredArgsConstructor
@Tag(name = "Combustibles", description = "Gestión de cargas de combustible para maquinaria, vehículos y motocicletas")
public class FuelLogController {

    private final CreateFuelLogUseCase createFuelLogUseCase;
    private final GetFuelHistoryUseCase getFuelHistoryUseCase;
    private final GetFuelDashboardUseCase getFuelDashboardUseCase;

    @PostMapping
    @Operation(summary = "Registrar carga de combustible")
    @PreAuthorize("hasAnyRole('OPERARIO','ADMIN')")
    public ResponseEntity<FuelLogResponse> create(@RequestBody FuelLogRequest request) {
        FuelLogResponse saved = createFuelLogUseCase.createFuelLog(request);
        return ResponseEntity.created(URI.create("/api/v1/fuel/" + saved.id())).body(saved);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener registro de combustible por ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FuelLogResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(getFuelHistoryUseCase.getById(id));
    }

    @GetMapping("/asset/{assetType}/{assetId}")
    @Operation(summary = "Historial de combustible de un activo")
    @PreAuthorize("hasAnyRole('OPERARIO','ADMIN','ACEITE')")
    public ResponseEntity<List<FuelLogResponse>> getByAsset(
            @PathVariable String assetType,
            @PathVariable Long assetId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        List<FuelLogResponse> result = (from != null && to != null)
                ? getFuelHistoryUseCase.getHistoryByAssetAndDateRange(assetType, assetId, from, to)
                : getFuelHistoryUseCase.getHistoryByAsset(assetType, assetId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard consolidado de combustible")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FuelDashboardResponse> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(getFuelDashboardUseCase.getDashboard(from, to));
    }

    @GetMapping
    @Operation(summary = "Listar todos los registros de combustible")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FuelLogResponse>> getAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(getFuelDashboardUseCase.getAllLogs(from, to));
    }

    @GetMapping("/ranking")
    @Operation(summary = "Ranking de activos por consumo en un período")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FuelLogResponse>> getRanking(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(getFuelDashboardUseCase.getRanking(from, to));
    }

    @GetMapping("/anomalies")
    @Operation(summary = "Registros marcados como anómalos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FuelLogResponse>> getAnomalies() {
        return ResponseEntity.ok(getFuelDashboardUseCase.getAnomalies());
    }

    @PatchMapping("/{id}/invoice")
    @Operation(summary = "Aprobar o rechazar factura de combustible")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FuelLogResponse> updateInvoiceStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(getFuelHistoryUseCase.updateInvoiceStatus(id, status));
    }

    @GetMapping("/stats/monthly")
    @Operation(summary = "Estadísticas mensuales de consumo y costo por tipo de activo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FuelMonthlyStatsResponse>> getMonthlyStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(getFuelDashboardUseCase.getMonthlyStats(from, to));
    }

    @GetMapping("/stats/historical")
    @Operation(summary = "Estadísticas históricas (usa tabla materializada para períodos > 90 días)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FuelMonthlyStatsResponse>> getHistoricalStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(getFuelDashboardUseCase.getHistoricalStats(from, to));
    }

    @PostMapping(value = "/{id}/invoice/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir foto o PDF de factura para un registro de combustible")
    @PreAuthorize("hasAnyRole('OPERARIO','ADMIN')")
    public ResponseEntity<FuelLogResponse> uploadInvoice(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(getFuelHistoryUseCase.uploadInvoiceFile(id, file));
    }

    @PatchMapping("/{id}/anomaly")
    @Operation(summary = "Descartar anomalía de un registro (y opcionalmente corregir el costo)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FuelLogResponse> dismissAnomaly(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String reason = body.containsKey("reason") ? String.valueOf(body.get("reason")) : null;
        BigDecimal correctedCost = body.containsKey("totalCostActual") && body.get("totalCostActual") != null
                ? new BigDecimal(String.valueOf(body.get("totalCostActual"))) : null;
        return ResponseEntity.ok(getFuelHistoryUseCase.dismissAnomaly(id, reason, correctedCost));
    }

    @GetMapping("/export")
    @Operation(summary = "Exportar todos los registros de combustible a Excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        try {
            byte[] data = getFuelDashboardUseCase.exportToExcel(from, to);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"combustibles.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
