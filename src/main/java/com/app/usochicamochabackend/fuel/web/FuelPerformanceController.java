package com.app.usochicamochabackend.fuel.web;

import com.app.usochicamochabackend.fuel.application.dto.FuelPerformanceResponse;
import com.app.usochicamochabackend.fuel.application.port.GetFuelPerformanceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR_OPERATIVO', 'ADMIN')")
    public ResponseEntity<List<FuelPerformanceResponse>> rendimiento(
            @RequestParam String tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(getFuelPerformanceUseCase.obtenerRendimiento(tipo, fechaInicio, fechaFin));
    }
}
