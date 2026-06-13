package com.app.usochicamochabackend.moto.web;

import com.app.usochicamochabackend.moto.application.dto.MotoMonitoringDTO;
import com.app.usochicamochabackend.moto.application.port.MotoMonitoringUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/moto/monitoring")
@RequiredArgsConstructor
@Tag(
    name = "Moto Monitoring",
    description = "Tablero consolidado para motocicletas: documentación, aceite, ubicación e inspecciones"
)
public class MotoMonitoringController {

    private final MotoMonitoringUseCase motoMonitoringUseCase;

    @GetMapping("/consolidated")
    @Operation(
        summary = "Monitoreo consolidado (motocicletas)",
        description = "Una fila por motocicleta: vigencias de documentos, estado de aceite y datos derivados de última inspección"
    )
    public ResponseEntity<List<MotoMonitoringDTO>> getConsolidated() {
        return ResponseEntity.ok(motoMonitoringUseCase.getConsolidatedMonitoring());
    }

    @GetMapping("/alerts/yellow")
    @Operation(
        summary = "Motocicletas con alertas YELLOW (próximas a cambio de aceite)",
        description = "Filtra motocicletas con 80-99% de uso del intervalo de cambio de aceite"
    )
    public ResponseEntity<List<MotoMonitoringDTO>> getYellowAlerts() {
        List<MotoMonitoringDTO> all = motoMonitoringUseCase.getConsolidatedMonitoring();
        List<MotoMonitoringDTO> yellow = all.stream()
            .filter(m -> m.oil() != null
                && m.oil().alertColor() != null
                && "YELLOW".equals(m.oil().alertColor()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(yellow);
    }

    @GetMapping("/alerts/red")
    @Operation(
        summary = "Motocicletas con alertas RED (cambio de aceite URGENTE)",
        description = "Filtra motocicletas con 100%+ de uso del intervalo de cambio de aceite"
    )
    public ResponseEntity<List<MotoMonitoringDTO>> getRedAlerts() {
        List<MotoMonitoringDTO> all = motoMonitoringUseCase.getConsolidatedMonitoring();
        List<MotoMonitoringDTO> red = all.stream()
            .filter(m -> m.oil() != null
                && m.oil().alertColor() != null
                && "RED".equals(m.oil().alertColor()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(red);
    }
}
