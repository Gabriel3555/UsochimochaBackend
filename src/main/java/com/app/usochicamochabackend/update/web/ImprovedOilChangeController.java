package com.app.usochicamochabackend.update.web;

import com.app.usochicamochabackend.update.application.dto.*;
import com.app.usochicamochabackend.update.infrastructure.entity.*;
import com.app.usochicamochabackend.update.infrastructure.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador mejorado para gestionar cambios de aceite y análisis SOS
 * con sistema basado en uso (km/horas) en lugar de fechas
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/improved-oil-changes")
@Tag(name = "Improved Oil Changes", description = "Sistema mejorado de cambios de aceite basado en uso")
@RequiredArgsConstructor
public class ImprovedOilChangeController {

    private final VehicleOilChangeRepository vehicleOilChangeRepository;
    private final OilChangeRepository oilChangeRepository;
    private final OilChangeRequirementRepository requirementRepository;
    private final OilAnalysisSosRepository sosRepository;
    private final com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository machineRepository;

    @PostMapping("/vehicle")
    @PreAuthorize("hasAnyRole('OPERARIO', 'SUPERVISOR_OPERATIVO', 'ADMIN')")
    @Operation(summary = "Crear cambio de aceite para vehículo o motocicleta")
    public ResponseEntity<VehicleOilChangeResponseDTO> createVehicleOilChange(
            @Valid @RequestBody CreateVehicleOilChangeDTO dto) {
        log.info("🛢️ Creando cambio de aceite para vehículo: {}", dto.placa());

        try {
            OilType oilType = OilType.valueOf(dto.oilType());
            OilChangeRequirementEntity requirement = requirementRepository.findById(dto.requirementId())
                    .orElseThrow(() -> new IllegalArgumentException("Requisito no encontrado"));

            int percentageUsed = 0;

            VehicleOilChangeEntity entity = VehicleOilChangeEntity.builder()
                    .oilType(oilType)
                    .kmAtChange(dto.kmAtChange())
                    .quantity(dto.quantity())
                    .airFilterChanged(dto.airFilterChanged())
                    .dateStamp(LocalDateTime.now())
                    .build();

            VehicleOilChangeEntity saved = vehicleOilChangeRepository.save(entity);
            log.info("✅ Cambio de aceite creado con ID: {}", saved.getId());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(VehicleOilChangeResponseDTO.fromEntity(saved));

        } catch (IllegalArgumentException e) {
            log.error("❌ Error creando cambio de aceite: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/machine")
    @PreAuthorize("hasAnyRole('OPERARIO', 'SUPERVISOR_OPERATIVO', 'ADMIN')")
    @Operation(summary = "Crear cambio de aceite para maquinaria")
    public ResponseEntity<MachineOilChangeResponseDTO> createMachineOilChange(
            @Valid @RequestBody CreateMachineOilChangeDTO dto) {
        log.info("🛢️ Creando cambio de aceite para máquina: {}", dto.machineId());

        try {
            OilType oilType = OilType.valueOf(dto.oilType());
            OilChangeRequirementEntity requirement = requirementRepository.findById(dto.requirementId())
                    .orElseThrow(() -> new IllegalArgumentException("Requisito no encontrado"));

            int percentageUsed = 0;

            OilChangeEntity entity = OilChangeEntity.builder()
                    .hourStamp(dto.hourStamp())
                    .oilType(oilType)
                    .quantity(dto.quantity())
                    .motorOil(dto.motorOil())
                    .hydraulicOil(dto.hydraulicOil())
                    .requirement(requirement)
                    .percentageUsed(percentageUsed)
                    .dateStamp(LocalDateTime.now())
                    .build();

            OilChangeEntity saved = oilChangeRepository.save(entity);
            log.info("✅ Cambio de aceite de maquinaria creado con ID: {}", saved.getId());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(MachineOilChangeResponseDTO.fromEntity(saved));

        } catch (IllegalArgumentException e) {
            log.error("❌ Error creando cambio de aceite: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/machine/simple")
    @PreAuthorize("hasAnyRole('OPERARIO', 'SUPERVISOR_OPERATIVO', 'ADMIN')")
    @Operation(summary = "Crear cambio de aceite simplificado (sin requisito)")
    public ResponseEntity<?> createSimpleMachineOilChange(
            @RequestParam Long machineId,
            @RequestParam Integer hourStamp,
            @RequestParam Integer averageHoursChange,
            @RequestParam(defaultValue = "true") Boolean motorOil,
            @RequestParam(defaultValue = "false") Boolean hydraulicOil,
            @RequestParam(defaultValue = "50") Double quantity) {
        log.info("🛢️ Creando cambio de aceite simplificado para máquina ID: {}, horas: {}", machineId, hourStamp);

        try {
            var machine = machineRepository.findById(machineId)
                    .orElseThrow(() -> new IllegalArgumentException("Máquina no encontrada"));

            OilChangeEntity entity = OilChangeEntity.builder()
                    .machine(machine)
                    .hourStamp(hourStamp)
                    .averageHoursChange(averageHoursChange)
                    .motorOil(motorOil)
                    .hydraulicOil(hydraulicOil)
                    .quantity(quantity)
                    .dateStamp(LocalDateTime.now())
                    .build();

            OilChangeEntity saved = oilChangeRepository.save(entity);
            log.info("✅ Cambio de aceite simplificado creado con ID: {}", saved.getId());

            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (IllegalArgumentException e) {
            log.error("❌ Error creando cambio de aceite: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/requirements")
    @PreAuthorize("hasAnyRole('OPERARIO', 'SUPERVISOR_OPERATIVO', 'ADMIN')")
    @Operation(summary = "Obtener requisitos de cambio de aceite")
    public ResponseEntity<List<OilChangeRequirementDTO>> getOilChangeRequirements(
            @RequestParam(required = false) String assetType) {
        log.debug("📋 Obteniendo requisitos de cambio de aceite");

        List<OilChangeRequirementEntity> requirements;
        if (assetType != null) {
            AssetType type = AssetType.valueOf(assetType);
            requirements = requirementRepository.findByAssetTypeForDisplay(type);
        } else {
            requirements = requirementRepository.findByActiveTrue();
        }

        return ResponseEntity.ok(
                requirements.stream()
                        .map(OilChangeRequirementDTO::fromEntity)
                        .toList()
        );
    }

    @PostMapping("/analysis-sos")
    @PreAuthorize("hasAnyRole('OPERARIO', 'SUPERVISOR_OPERATIVO', 'ADMIN')")
    @Operation(summary = "Crear análisis SOS para maquinaria")
    public ResponseEntity<OilAnalysisSosDTO> createOilAnalysisSos(
            @Valid @RequestBody CreateOilAnalysisSosDTO dto) {
        log.info("🔬 Creando análisis SOS para máquina: {}", dto.machineName());

        try {
            OilType oilType = OilType.valueOf(dto.oilType());

            OilAnalysisSosEntity entity = OilAnalysisSosEntity.builder()
                    .machineId(dto.machineId())
                    .machineName(dto.machineName())
                    .oilType(oilType)
                    .analysisDate(LocalDateTime.now())
                    .hoursAtAnalysis(dto.hoursAtAnalysis())
                    .nextChangeHours(dto.nextChangeHours())
                    .sosReportUrl(dto.sosReportUrl())
                    .approvedByMechanic(dto.approvedByMechanic())
                    .observations(dto.observations())
                    .isApproved(false)
                    .build();

            OilAnalysisSosEntity saved = sosRepository.save(entity);
            log.info("✅ Análisis SOS creado con ID: {}", saved.getId());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(OilAnalysisSosDTO.fromEntity(saved));

        } catch (IllegalArgumentException e) {
            log.error("❌ Error creando análisis SOS: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/analysis-sos/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPERVISOR_OPERATIVO', 'ADMIN')")
    @Operation(summary = "Aprobar análisis SOS")
    public ResponseEntity<OilAnalysisSosDTO> approveSosAnalysis(@PathVariable Long id) {
        log.info("✅ Aprobando análisis SOS: {}", id);

        Optional<OilAnalysisSosEntity> sos = sosRepository.findById(id);
        if (sos.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        OilAnalysisSosEntity entity = sos.get();
        entity.setIsApproved(true);
        OilAnalysisSosEntity saved = sosRepository.save(entity);

        log.info("✅ Análisis SOS aprobado: {}", id);
        return ResponseEntity.ok(OilAnalysisSosDTO.fromEntity(saved));
    }

    @GetMapping("/analysis-sos/machine/{machineId}")
    @PreAuthorize("hasAnyRole('OPERARIO', 'SUPERVISOR_OPERATIVO', 'ADMIN')")
    @Operation(summary = "Obtener análisis SOS de una máquina")
    public ResponseEntity<List<OilAnalysisSosDTO>> getMachineAnalyses(
            @PathVariable Long machineId,
            @RequestParam(required = false, defaultValue = "false") boolean approvedOnly) {
        log.debug("📋 Obteniendo análisis SOS para máquina: {}", machineId);

        List<OilAnalysisSosEntity> analyses;
        if (approvedOnly) {
            analyses = sosRepository.findByMachineIdOrderByAnalysisDateDesc(machineId)
                    .stream()
                    .filter(OilAnalysisSosEntity::getIsApproved)
                    .toList();
        } else {
            analyses = sosRepository.findByMachineIdOrderByAnalysisDateDesc(machineId);
        }

        return ResponseEntity.ok(
                analyses.stream()
                        .map(OilAnalysisSosDTO::fromEntity)
                        .toList()
        );
    }

    @GetMapping("/analysis-sos/pending/{machineId}")
    @PreAuthorize("hasAnyRole('SUPERVISOR_OPERATIVO', 'ADMIN')")
    @Operation(summary = "Obtener análisis SOS pendientes de aprobación")
    public ResponseEntity<List<OilAnalysisSosDTO>> getPendingAnalyses(@PathVariable Long machineId) {
        log.debug("📋 Obteniendo análisis SOS pendientes para máquina: {}", machineId);

        List<OilAnalysisSosEntity> pending = sosRepository.findPendingApprovalByMachineId(machineId);

        return ResponseEntity.ok(
                pending.stream()
                        .map(OilAnalysisSosDTO::fromEntity)
                        .toList()
        );
    }
}
