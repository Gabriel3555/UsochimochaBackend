package com.app.usochicamochabackend.notifications.application;

import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.machine.infrastructure.entity.MachineEntity;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
import com.app.usochicamochabackend.notifications.application.dto.AlertDTO;
import com.app.usochicamochabackend.notifications.infrastructure.entity.AlertEntity;
import com.app.usochicamochabackend.notifications.infrastructure.repository.AlertRepository;
import com.app.usochicamochabackend.update.infrastructure.entity.*;
import com.app.usochicamochabackend.update.infrastructure.repository.*;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Servicio mejorado para calcular alertas de cambio de aceite basadas en:
 * - KILOMETRAJE para vehículos y motocicletas
 * - HOROMETRO para maquinaria
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImprovedOilChangeAlertService {

    private final AlertRepository alertRepository;
    private final VehicleOilChangeRepository vehicleOilChangeRepository;
    private final OilChangeRepository oilChangeRepository;
    private final OilChangeRequirementRepository requirementRepository;
    private final VehicleRepository vehicleRepository;
    private final MachineRepository machineRepository;
    private final NotificationService notificationService;

    /**
     * Calcula alerta de cambio de aceite para VEHÍCULOS Y MOTOCICLETAS
     * basado en el kilometraje actual vs. próximo cambio recomendado
     */
    public void checkOilChangeAlertForVehicleByKm(String placa) {
        log.debug("🛢️ Calculando alerta de cambio de aceite para: {} (por kilometraje)", placa);

        // Obtener último cambio de aceite
        var lastChanges = vehicleOilChangeRepository.findAllByPlacaOrderByDateStampDesc(placa);
        if (lastChanges.isEmpty()) {
            log.debug("⚠️ Sin registro de cambio de aceite anterior para: {}", placa);
            createFirstOilChangeAlert(placa);
            return;
        }

        VehicleOilChangeEntity lastChange = lastChanges.get(0);

        // Obtener vehículo actual (km actual)
        Optional<VehicleEntity> vehiculo = vehicleRepository.findByPlaca(placa);
        if (vehiculo.isEmpty()) {
            log.debug("🚗 Vehículo no encontrado: {}", placa);
            return;
        }

        Integer currentKm = vehiculo.get().getKilometrajeActual();
        if (currentKm == null) currentKm = 0;

        // Obtener intervalo de cambio (configurado en el último cambio)
        Integer intervalKm = lastChange.getIntervalKm();
        if (intervalKm == null || intervalKm <= 0) {
            log.warn("⚠️ Sin intervalo configurado para: {}", placa);
            return;
        }

        Integer nextChangeKm = lastChange.getKmAtChange() + intervalKm;
        Integer kmSinceLastChange = currentKm - lastChange.getKmAtChange();
        Integer totalKmRange = intervalKm;
        int percentageUsed = totalKmRange > 0 ? (kmSinceLastChange * 100) / totalKmRange : 0;

        log.debug("📊 {} - Km actual: {}, Último cambio: {}km, Próximo: {}km, Uso: {}%",
            placa, currentKm, lastChange.getKmAtChange(), nextChangeKm, percentageUsed);

        // Crear alertas por RANGO DE USO
        AlertEntity alert = AlertEntity.builder()
            .placa(placa)
            .tipoAlerta("CAMBIO_ACEITE")
            .metric("KILOMETERS")
            .colorEstado(null) // Será calculado por estado
            .build();

        if (percentageUsed >= 100) {
            log.warn("Cambio de aceite VENCIDO para: {} ({}%)", placa, percentageUsed);
            alert.setDescripcion(String.format(
                "CAMBIO DE ACEITE VENCIDO\nUso: %d%%\nPróximo cambio: YA PASADO",
                percentageUsed
            ));
            alert.setColorEstado("RED");
        }
        else if (percentageUsed >= 80) {
            log.warn("Próximo cambio de aceite para: {} ({}%)", placa, percentageUsed);
            alert.setDescripcion(String.format(
                "PRÓXIMO CAMBIO DE ACEITE\nUso: %d%%\nFaltan: %d km",
                percentageUsed, (nextChangeKm - currentKm)
            ));
            alert.setColorEstado("YELLOW");
        }
        else if (percentageUsed >= 60) {
            log.info("Cambio de aceite programado para: {} ({}%)", placa, percentageUsed);
            alert.setDescripcion(String.format(
                "CAMBIO PROGRAMADO\nUso: %d%%\nFaltan: %d km",
                percentageUsed, (nextChangeKm - currentKm)
            ));
            alert.setColorEstado("BLUE");
        }
        else {
            log.debug("Aceite en buen estado para: {} ({}%)", placa, percentageUsed);
            alert.setDescripcion(String.format(
                "ACEITE EN BUEN ESTADO\nUso: %d%%\nFaltan: %d km",
                percentageUsed, (nextChangeKm - currentKm)
            ));
            alert.setColorEstado("GREEN");
        }

        // Guardar alerta
        saveAlert(alert);
    }

    /**
     * Calcula alerta de cambio de aceite para MAQUINARIA
     * basado en el horometro actual vs. próximo cambio recomendado
     * Considera análisis SOS si existen
     */
    public void checkOilChangeAlertForMachineByHours(Long machineId, String machineName, boolean isMotorOil) {
        log.debug("🛢️ Calculando alerta de cambio de aceite para máquina: {} ({})",
            machineName, isMotorOil ? "motor" : "hidráulico");

        // Obtener último cambio de aceite
        OilChangeEntity lastChange = isMotorOil
            ? oilChangeRepository.getLastMotorOilChangeByMachineId(machineId)
            : oilChangeRepository.getLastHydraulicOilChangeByMachineId(machineId);

        if (lastChange == null || lastChange.getHourStamp() == null) {
            log.debug("⚠️ Sin registro de cambio de aceite anterior para máquina: {}", machineName);
            createFirstOilChangeAlertMachinery(machineName, isMotorOil);
            return;
        }

        // Obtener máquina actual (horas actuales)
        MachineEntity machine = machineRepository.findById(machineId)
            .orElseThrow(() -> new ResourceNotFoundException("Máquina no encontrada: " + machineId));
        Integer currentHours = machine.getHorometroActual() != null ? machine.getHorometroActual() : 0;

        // Obtener requisito de cambio
        OilChangeRequirementEntity requirement = lastChange.getRequirement();
        if (requirement == null) {
            log.warn("⚠️ Sin requisito de cambio configurado para máquina: {}", machineName);
            return;
        }

        // Calcular horas desde el último cambio
        int hoursSinceLastChange = currentHours - lastChange.getHourStamp();
        int totalHourRange = requirement.getHourRange();
        double percentageUsed = (hoursSinceLastChange * 100.0) / totalHourRange;

        log.debug("🛢️ Máquina: {} - Horas actuales: {} - Horas desde cambio: {} - Intervalo: {} horas - Uso: {:.1f}%",
            machineName, currentHours, hoursSinceLastChange, totalHourRange, percentageUsed);

        // Determinar estado y crear alerta
        AlertEntity alert = AlertEntity.builder()
            .machineName(machineName)
            .machineId(machineId)
            .tipoAlerta("CAMBIO_ACEITE")
            .metric("HOURS")
            .subtipo(isMotorOil ? "MOTOR" : "HYDRAULIC")
            .build();

        if (percentageUsed >= 100) {
            alert.setColorEstado("RED");
            alert.setDescripcion(String.format("%s DE ACEITE %s VENCIDO\n" +
                "Horas: %d (Intervalo: %d hrs | Uso: %.1f%%)\n" +
                "Se requiere cambio inmediato",
                isMotorOil ? "CAMBIO" : "CAMBIO", isMotorOil ? "MOTOR" : "HIDRÁULICO",
                currentHours, totalHourRange, percentageUsed));
        } else if (percentageUsed >= 80) {
            alert.setColorEstado("YELLOW");
            alert.setDescripcion(String.format("PRÓXIMO CAMBIO DE ACEITE %s\n" +
                "Horas: %d (Intervalo: %d hrs | Uso: %.1f%%)\n" +
                "Próximo cambio urgente",
                isMotorOil ? "MOTOR" : "HIDRÁULICO",
                currentHours, totalHourRange, percentageUsed));
        } else if (percentageUsed >= 60) {
            alert.setColorEstado("BLUE");
            alert.setDescripcion(String.format("CAMBIO PROGRAMADO DE ACEITE %s\n" +
                "Horas: %d (Intervalo: %d hrs | Uso: %.1f%%)\n" +
                "Próximo cambio en calendario",
                isMotorOil ? "MOTOR" : "HIDRÁULICO",
                currentHours, totalHourRange, percentageUsed));
        } else {
            alert.setColorEstado("GREEN");
            alert.setDescripcion(String.format("ACEITE %s EN BUEN ESTADO\n" +
                "Horas: %d (Intervalo: %d hrs | Uso: %.1f%%)",
                isMotorOil ? "MOTOR" : "HIDRÁULICO",
                currentHours, totalHourRange, percentageUsed));
        }

        saveAlert(alert);
        notificationService.notifyAlert(AlertDTO.fromEntity(alert));
    }

    /**
     * Crea alerta para primer cambio de aceite (cuando no hay historial)
     */
    private void createFirstOilChangeAlert(String placa) {
        log.info("Creando alerta de primer cambio de aceite para: {}", placa);

        AlertEntity alert = AlertEntity.builder()
            .placa(placa)
            .tipoAlerta("CAMBIO_ACEITE")
            .metric("KILOMETERS")
            .descripcion("PRIMER CAMBIO DE ACEITE RECOMENDADO\nRecordatorio: Realiza el primer cambio de aceite según manual del fabricante")
            .colorEstado("RED")
            .build();

        saveAlert(alert);
    }

    /**
     * Crea alerta para primer cambio de aceite en máquina
     */
    private void createFirstOilChangeAlertMachinery(String machineName, boolean isMotorOil) {
        log.info("Creando alerta de primer cambio de aceite para máquina: {} ({})",
            machineName, isMotorOil ? "motor" : "hidráulico");

        String description = isMotorOil
            ? "PRIMER CAMBIO DE ACEITE MOTOR RECOMENDADO"
            : "PRIMER CAMBIO DE ACEITE HIDRÁULICO RECOMENDADO";

        log.debug("Alerta de maquinaria: {}", description);
    }

    /**
     * Guarda alerta en BD, actualizando si ya existe del mismo tipo
     */
    private void saveAlert(AlertEntity alert) {
        try {
            // Validar que no exista alerta activa del mismo tipo
            var existingAlert = alertRepository
                .findTopByPlacaAndTipoAlertaAndEstadoOrderByFechaCreacionDesc(
                    alert.getPlaca(),
                    alert.getTipoAlerta(),
                    "ACTIVA"
                );

            if (existingAlert.isPresent()) {
                AlertEntity existing = existingAlert.get();
                existing.setDescripcion(alert.getDescripcion());
                existing.setColorEstado(alert.getColorEstado());
                existing.setFechaActualizacion(LocalDateTime.now());
                alertRepository.save(existing);
                log.debug("Alerta actualizada para: {}", alert.getPlaca());
            } else {
                alert.setFechaCreacion(LocalDateTime.now());
                alert.setEstado("ACTIVA");
                alertRepository.save(alert);
                log.info("Alerta creada para: {}", alert.getPlaca());
            }

            // Notificar en tiempo real
            notificationService.notifyAlert(AlertDTO.fromEntity(alert));

        } catch (Exception e) {
            log.error("❌ Error guardando alerta para: {}", alert.getPlaca(), e);
        }
    }
}
