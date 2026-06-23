package com.app.usochicamochabackend.machine.application.service;

import com.app.usochicamochabackend.machine.application.dto.MachineMonitoringDTO;
import com.app.usochicamochabackend.machine.infrastructure.entity.MachineEntity;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
import com.app.usochicamochabackend.review.infrastructure.entity.InspectionEntity;
import com.app.usochicamochabackend.review.infrastructure.repository.InspectionRepository;
import com.app.usochicamochabackend.shared.calculator.OilChangeAlertCalculator;
import com.app.usochicamochabackend.shared.dto.AlertStatus;
import com.app.usochicamochabackend.update.infrastructure.entity.OilChangeEntity;
import com.app.usochicamochabackend.update.infrastructure.repository.OilChangeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de monitoreo consolidado de cambios de aceite para maquinaria.
 *
 * Soporta 2 tipos de aceite:
 * - Motor (motorOil=true)
 * - Hidráulico (hydraulicOil=true)
 */
@Service
@RequiredArgsConstructor
public class MachineMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(MachineMonitoringService.class);

    private final MachineRepository machineRepository;
    private final OilChangeRepository oilChangeRepository;
    private final InspectionRepository inspectionRepository;

    /**
     * Obtiene consolidado de aceite de TODAS las máquinas.
     */
    public List<MachineMonitoringDTO> getConsolidatedMonitoring() {
        logger.info("Obteniendo consolidado de cambios de aceite para todas las máquinas");
        try {
            List<MachineEntity> machines = machineRepository.findAll();
            List<MachineMonitoringDTO> result = new ArrayList<>();

            for (MachineEntity machine : machines) {
                if (machine.getStatus() != null && machine.getStatus()) {  // Solo máquinas activas
                    MachineMonitoringDTO dto = buildMonitoringDTO(machine);
                    result.add(dto);
                }
            }

            logger.info("Se obtuvieron {} máquinas con consolidado de aceite", result.size());
            return result;

        } catch (Exception e) {
            logger.error("Error al obtener consolidado de máquinas", e);
            throw e;
        }
    }

    /**
     * Obtiene consolidado de aceite (motor + hidráulico) para UNA máquina.
     */
    public MachineMonitoringDTO getConsolidateById(Long machineId) {
        logger.debug("Obteniendo consolidado de aceite para máquina: {}", machineId);

        Optional<MachineEntity> machine = machineRepository.findById(machineId);
        if (machine.isEmpty()) {
            logger.warn("Máquina no encontrada: {}", machineId);
            return null;
        }

        return buildMonitoringDTO(machine.get());
    }

    /**
     * Construye el DTO de monitoreo para una máquina.
     * IMPORTANTE: El horómetro se obtiene de la última INSPECCIÓN, no de machine.horometroActual
     */
    private MachineMonitoringDTO buildMonitoringDTO(MachineEntity machine) {
        // Obtener el horómetro actual desde la última inspección (como lo hace OilChangeService)
        InspectionEntity lastInspection = inspectionRepository.getLastInspection(machine.getId());
        Integer horometroActual = (lastInspection != null && lastInspection.getHourMeter() != null)
            ? lastInspection.getHourMeter().intValue()
            : (machine.getHorometroActual() != null ? machine.getHorometroActual() : 0);

        logger.info("✅ [MONITORING] Máquina: {}, Horómetro: {} (from inspection: {})",
            machine.getName(), horometroActual, lastInspection != null);

        // Obtener cambios más recientes de CADA tipo de aceite
        OilChangeEntity lastMotorOil = oilChangeRepository.getLastMotorOilChangeByMachineId(machine.getId());
        OilChangeEntity lastHydraulicOil = oilChangeRepository.getLastHydraulicOilChangeByMachineId(machine.getId());

        // Calcular alertas para cada tipo
        MachineMonitoringDTO.OilChangeInfoDTO motorOilInfo = null;
        MachineMonitoringDTO.OilChangeInfoDTO hydraulicOilInfo = null;

        if (lastMotorOil != null) {
            motorOilInfo = buildOilInfo("Motor Oil", lastMotorOil, horometroActual);
        }

        if (lastHydraulicOil != null) {
            hydraulicOilInfo = buildOilInfo("Hydraulic Oil", lastHydraulicOil, horometroActual);
        }

        return new MachineMonitoringDTO(
            machine.getId(),
            machine.getName(),
            horometroActual,
            motorOilInfo,
            hydraulicOilInfo,
            null  // lastInspectionDate (si aplica, obtener de inspecciones)
        );
    }

    /**
     * Construye la información de un tipo de aceite específico con cálculo de alerta.
     */
    private MachineMonitoringDTO.OilChangeInfoDTO buildOilInfo(
        String oilTypeLabel,
        OilChangeEntity oilChange,
        Integer currentHours
    ) {
        // Obtener intervalo configurado (en horas)
        Integer intervalHours = oilChange.getAverageHoursChange();
        Integer hoursLastChange = oilChange.getHourStamp() != null ? oilChange.getHourStamp() : 0;

        if (intervalHours == null || intervalHours <= 0) {
            logger.warn("Intervalo inválido para máquina {}: {}", oilChange.getMachine().getId(), oilChange.getId());
            return new MachineMonitoringDTO.OilChangeInfoDTO(
                oilTypeLabel,
                oilChange.getBrand() != null ? oilChange.getBrand().getName() : "Desconocida",
                oilChange.getQuantity(),
                hoursLastChange,
                intervalHours,
                -1.0,  // % desconocido
                "GRAY", // Color desconocido
                "Intervalo no configurado"
            );
        }

        // Cálculo de alerta usando OilChangeAlertCalculator
        long hoursSinceChange = currentHours - hoursLastChange;
        AlertStatus alertStatus = OilChangeAlertCalculator.calculateAlert(hoursSinceChange, intervalHours.longValue());

        return new MachineMonitoringDTO.OilChangeInfoDTO(
            oilTypeLabel,
            oilChange.getBrand() != null ? oilChange.getBrand().getName() : "Desconocida",
            oilChange.getQuantity(),
            hoursLastChange,
            intervalHours,
            alertStatus.percentageUsed(),
            alertStatus.color(),
            alertStatus.message()
        );
    }
}
