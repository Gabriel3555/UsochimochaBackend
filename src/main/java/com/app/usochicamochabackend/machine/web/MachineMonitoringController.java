package com.app.usochicamochabackend.machine.web;

import com.app.usochicamochabackend.machine.application.dto.MachineMonitoringDTO;
import com.app.usochicamochabackend.machine.application.service.MachineMonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller REST para monitoreo de cambios de aceite en maquinaria.
 */
@RestController
@RequestMapping("/api/v1/machine/monitoring")
@RequiredArgsConstructor
@Tag(name = "Machine Monitoring", description = "Tablero consolidado de maquinaria: aceites, horas, estado operativo")
public class MachineMonitoringController {

    private final MachineMonitoringService machineMonitoringService;

    /**
     * Obtiene consolidado de aceite de TODAS las máquinas.
     */
    @GetMapping("/consolidated")
    @Operation(
        summary = "Obtener consolidado de aceite para todas las máquinas",
        description = "Devuelve lista de máquinas con estado de aceite motor e hidráulico"
    )
    public ResponseEntity<List<MachineMonitoringDTO>> getConsolidated() {
        List<MachineMonitoringDTO> result = machineMonitoringService.getConsolidatedMonitoring();
        return ResponseEntity.ok(result);
    }

    /**
     * Obtiene consolidado de aceite para UNA máquina (motor + hidráulico).
     */
    @GetMapping("/consolidated/{machineId}")
    @Operation(
        summary = "Obtener consolidado de aceite para una máquina",
        description = "Devuelve detalle de aceite motor e hidráulico para máquina específica"
    )
    public ResponseEntity<MachineMonitoringDTO> getByMachineId(@PathVariable Long machineId) {
        MachineMonitoringDTO result = machineMonitoringService.getConsolidateById(machineId);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Obtiene todas las máquinas con alertas YELLOW (próximas a cambio).
     */
    @GetMapping("/alerts/yellow")
    @Operation(
        summary = "Máquinas con alertas YELLOW (próximas a cambio de aceite)",
        description = "Filtra máquinas con 80-99% de uso del intervalo"
    )
    public ResponseEntity<List<MachineMonitoringDTO>> getYellowAlerts() {
        List<MachineMonitoringDTO> all = machineMonitoringService.getConsolidatedMonitoring();
        List<MachineMonitoringDTO> yellow = all.stream()
            .filter(m -> isYellowAlert(m))
            .collect(Collectors.toList());
        return ResponseEntity.ok(yellow);
    }

    /**
     * Obtiene todas las máquinas con alertas RED (cambio urgente).
     */
    @GetMapping("/alerts/red")
    @Operation(
        summary = "Máquinas con alertas RED (cambio de aceite urgente)",
        description = "Filtra máquinas con 100%+ de uso del intervalo"
    )
    public ResponseEntity<List<MachineMonitoringDTO>> getRedAlerts() {
        List<MachineMonitoringDTO> all = machineMonitoringService.getConsolidatedMonitoring();
        List<MachineMonitoringDTO> red = all.stream()
            .filter(m -> isRedAlert(m))
            .collect(Collectors.toList());
        return ResponseEntity.ok(red);
    }

    /**
     * Verifica si una máquina tiene alerta YELLOW (80-99% en cualquier aceite).
     */
    private boolean isYellowAlert(MachineMonitoringDTO machine) {
        boolean motorYellow = machine.motorOilInfo() != null
            && "YELLOW".equals(machine.motorOilInfo().alertColor());
        boolean hydraulicYellow = machine.hydraulicOilInfo() != null
            && "YELLOW".equals(machine.hydraulicOilInfo().alertColor());
        return motorYellow || hydraulicYellow;
    }

    /**
     * Verifica si una máquina tiene alerta RED (100%+ en cualquier aceite).
     */
    private boolean isRedAlert(MachineMonitoringDTO machine) {
        boolean motorRed = machine.motorOilInfo() != null
            && "RED".equals(machine.motorOilInfo().alertColor());
        boolean hydraulicRed = machine.hydraulicOilInfo() != null
            && "RED".equals(machine.hydraulicOilInfo().alertColor());
        return motorRed || hydraulicRed;
    }
}
