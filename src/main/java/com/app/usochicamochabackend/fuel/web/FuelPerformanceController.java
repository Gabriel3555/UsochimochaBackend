package com.app.usochicamochabackend.fuel.web;

import com.app.usochicamochabackend.fuel.application.dto.FuelPerformanceResponse;
import com.app.usochicamochabackend.fuel.application.port.ExportFuelMonthlyPerformanceUseCase;
import com.app.usochicamochabackend.fuel.application.port.ExportFuelPerformanceHistoryUseCase;
import com.app.usochicamochabackend.fuel.application.port.GetFuelPerformanceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/fuel/rendimiento")
@RequiredArgsConstructor
public class FuelPerformanceController {

    private final GetFuelPerformanceUseCase getFuelPerformanceUseCase;
    private final ExportFuelPerformanceHistoryUseCase exportFuelPerformanceHistoryUseCase;
    private final ExportFuelMonthlyPerformanceUseCase exportFuelMonthlyPerformanceUseCase;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR_OPERATIVO', 'ADMIN')")
    public ResponseEntity<List<FuelPerformanceResponse>> rendimiento(
            @RequestParam String tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(getFuelPerformanceUseCase.obtenerRendimiento(tipo, fechaInicio, fechaFin));
    }

    // Historial completo de UN activo, todo agrupado en un .xlsx con una hoja por mes
    // (ver FuelPerformanceHistoryExcelExportService) — botón "Exportar Excel" de la
    // pantalla de detalle de Rendimiento.
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('SUPERVISOR_OPERATIVO', 'ADMIN')")
    public ResponseEntity<byte[]> exportarHistorial(
            @RequestParam String tipo,
            @RequestParam Long activoId) {
        byte[] excelData = exportFuelPerformanceHistoryUseCase.exportarHistorialActivo(tipo, activoId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "rendimiento_historial.xlsx");
        return ResponseEntity.ok().headers(headers).body(excelData);
    }

    // Rendimiento mensual de TODOS los activos configurados, una hoja por mes con la
    // tabla de rendimiento real + consumo estándar de referencia (ver
    // FuelMonthlyPerformanceExcelExportService) — botón "Exportar Excel" de la
    // pantalla general de Rendimiento (FuelPerformance.svelte), no del detalle de un
    // activo puntual. Sin fechas, exporta el año actual completo hasta hoy.
    @GetMapping("/export-mensual")
    @PreAuthorize("hasAnyRole('SUPERVISOR_OPERATIVO', 'ADMIN')")
    public ResponseEntity<byte[]> exportarMensual(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        byte[] excelData = exportFuelMonthlyPerformanceUseCase.exportarMensual(fechaInicio, fechaFin);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "rendimiento_mensual.xlsx");
        return ResponseEntity.ok().headers(headers).body(excelData);
    }
}
