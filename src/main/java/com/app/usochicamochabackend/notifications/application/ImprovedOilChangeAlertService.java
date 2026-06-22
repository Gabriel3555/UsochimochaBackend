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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

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
     * Wrapper transaccional para calcular alerta de cambio de aceite
     * Se ejecuta en la transacción actual para poder ver cambios del km
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void checkAndNotifyOilChangeAlert(String placa) {
        try {
            log.info("🛢️ [ALERT] Iniciando cálculo de alerta de cambio de aceite para: {}", placa);
            checkOilChangeAlertForVehicleByKm(placa);
            log.info("✅ [ALERT] Cálculo completado para: {}", placa);
        } catch (Exception e) {
            log.error("❌ [ALERT] Error calculando alerta para: {}", placa, e);
        }
    }

    /**
     * Calcula alerta de cambio de aceite para VEHÍCULOS Y MOTOCICLETAS
     * basado en el kilometraje actual vs. próximo cambio recomendado
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void checkOilChangeAlertForVehicleByKm(String placa) {
        log.info("🛢️ [VEHICLE] Calculando alerta de cambio de aceite para: {}", placa);

        try {
            // Obtener último cambio de aceite
            var lastChanges = vehicleOilChangeRepository.findAllByPlacaOrderByDateStampDesc(placa);
            if (lastChanges.isEmpty()) {
                log.info("🆕 [VEHICLE] Sin registro anterior para: {} - Creando alerta de PRIMER CAMBIO", placa);
                createFirstOilChangeAlert(placa);
                return;
            }

            VehicleOilChangeEntity lastChange = lastChanges.get(0);
            log.info("✅ [VEHICLE] Último cambio encontrado para {}: {}km", placa, lastChange.getKmAtChange());

            // Obtener vehículo actual (km actual)
            Optional<VehicleEntity> vehiculo = vehicleRepository.findByPlaca(placa);
            if (vehiculo.isEmpty()) {
                log.warn("❌ [VEHICLE] Vehículo no encontrado: {}", placa);
                return;
            }

            Integer currentKm = vehiculo.get().getKilometrajeActual();
            if (currentKm == null) currentKm = 0;
            log.info("📊 [VEHICLE] {} - Km actual: {}", placa, currentKm);

            // Obtener intervalo de cambio (configurado en el último cambio)
            Integer intervalKm = lastChange.getIntervalKm();
            log.info("🔧 [VEHICLE] Intervalo: {} para {}", intervalKm, placa);
            if (intervalKm == null || intervalKm <= 0) {
                log.warn("⚠️ [VEHICLE] Sin intervalo configurado para: {}", placa);
                return;
        }

        Integer nextChangeKm = lastChange.getKmAtChange() + intervalKm;
        Integer kmRemaining = nextChangeKm - currentKm;
        Integer kmSinceLastChange = currentKm - lastChange.getKmAtChange();
        int percentageUsed = intervalKm > 0 ? (kmSinceLastChange * 100) / intervalKm : 0;
        int percentageRemaining = 100 - percentageUsed;

        log.info("📊 [VEHICLE] {} - Km actual: {}, Último cambio: {}km, Próximo: {}km, Quedan: {}km ({}% intervalo), Uso: {}%",
            placa, currentKm, lastChange.getKmAtChange(), nextChangeKm, kmRemaining, percentageRemaining, percentageUsed);

        // Crear alertas: RED >= 100%, YELLOW >= 70% (quedan 30% o menos del intervalo)
        // Determinar si es moto o vehículo
        String tipoMaquinaria = "VEHICULO";
        if (vehiculo.get().getTipoVehiculo() != null &&
            "MOTOCICLETA".equalsIgnoreCase(vehiculo.get().getTipoVehiculo().getNombreTipo())) {
            tipoMaquinaria = "MOTOCICLETA";
        }

        AlertEntity alert = AlertEntity.builder()
            .placa(placa)
            .tipoAlerta("CAMBIO_ACEITE")
            .metric("KILOMETERS")
            .tipoMaquinaria(tipoMaquinaria)
            .colorEstado(null)
            .build();

        if (percentageUsed >= 100) {
            log.warn("🔴 Cambio de aceite VENCIDO para: {} ({}% usado)", placa, percentageUsed);
            alert.setDescripcion(String.format(
                "🚗 %s\nCAMBIO DE ACEITE VENCIDO\nUso: %d%%\nYA PASADO: %d km",
                placa, percentageUsed, Math.abs(kmRemaining)
            ));
            alert.setColorEstado("ROJO");
            saveAlert(alert);
        }
        else if (percentageUsed >= 70) {
            log.warn("🟡 Próximo cambio de aceite para: {} ({}% usado, quedan {}% intervalo)", placa, percentageUsed, percentageRemaining);
            alert.setDescripcion(String.format(
                "🚗 %s\nPRÓXIMO CAMBIO DE ACEITE\nUso: %d%%\nQuedan: %d km",
                placa, percentageUsed, kmRemaining
            ));
            alert.setColorEstado("AMARILLO");
            saveAlert(alert);
        }
        else {
            log.debug("✅ Aceite en buen estado para: {} ({}% usado, quedan {}km)", placa, percentageUsed, kmRemaining);
            // NO GUARDAR alertas GREEN
        }
        } catch (Exception e) {
            log.error("❌ [VEHICLE] Error calculando alerta para {}: {}", placa, e.getMessage(), e);
        }
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
        double percentageRemaining = 100.0 - percentageUsed;
        int hoursRemaining = totalHourRange - hoursSinceLastChange;

        log.info("🛢️ [MACHINE] {} - Horas actuales: {} - Horas desde cambio: {} - Intervalo: {} horas - Quedan: {}hrs ({}% intervalo) - Uso: {:.1f}%",
            machineName, currentHours, hoursSinceLastChange, totalHourRange, hoursRemaining, percentageRemaining, percentageUsed);

        // Determinar estado y crear alerta: RED >= 100%, YELLOW >= 70% (quedan 30% o menos del intervalo)
        // Para máquinas: usar placa = machineName para que saveAlert() pueda buscar por placa
        AlertEntity alert = AlertEntity.builder()
            .placa(machineName)
            .machineName(machineName)
            .machineId(machineId)
            .tipoAlerta("CAMBIO_ACEITE")
            .metric("HOURS")
            .subtipo(isMotorOil ? "MOTOR" : "HYDRAULIC")
            .tipoMaquinaria("MAQUINARIA")
            .build();

        if (percentageUsed >= 100) {
            alert.setColorEstado("ROJO");
            alert.setDescripcion(String.format("🚜 %s (MAQUINARIA - %s)\nCAMBIO DE ACEITE VENCIDO\n" +
                "Horas: %d (Intervalo: %d hrs | Uso: %.1f%%)\nYA PASADO: %d hrs",
                machineName, isMotorOil ? "MOTOR" : "HIDRÁULICO",
                currentHours, totalHourRange, percentageUsed, Math.abs(hoursRemaining)));
            saveAlert(alert);
        } else if (percentageUsed >= 70) {
            alert.setColorEstado("AMARILLO");
            alert.setDescripcion(String.format("🚜 %s (MAQUINARIA - %s)\nPRÓXIMO CAMBIO DE ACEITE\n" +
                "Horas: %d (Intervalo: %d hrs | Uso: %.1f%%)\nQuedan: %d horas",
                machineName, isMotorOil ? "MOTOR" : "HIDRÁULICO",
                currentHours, totalHourRange, percentageUsed, hoursRemaining));
            saveAlert(alert);
        } else {
            log.debug("✅ Aceite %s en buen estado para máquina: %s (%.1f%% usado, faltan %d horas)",
                isMotorOil ? "motor" : "hidráulico", machineName, percentageUsed, hoursRemaining);
            // NO GUARDAR alertas GREEN
        }
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
     * Y envía por WebSocket si es CAMBIO_ACEITE
     */
    @Transactional(propagation = Propagation.REQUIRED)
    private void saveAlert(AlertEntity alert) {
        try {
            // Validar que no exista alerta activa del mismo tipo
            var existingAlert = alertRepository
                .findTopByPlacaAndTipoAlertaAndEstadoOrderByFechaCreacionDesc(
                    alert.getPlaca(),
                    alert.getTipoAlerta(),
                    "ACTIVA"
                );

            AlertEntity savedAlert;
            if (existingAlert.isPresent()) {
                AlertEntity existing = existingAlert.get();
                existing.setDescripcion(alert.getDescripcion());
                existing.setColorEstado(alert.getColorEstado());
                existing.setFechaActualizacion(LocalDateTime.now());
                savedAlert = alertRepository.save(existing);
                log.debug("Alerta actualizada para: {}", alert.getPlaca());
            } else {
                alert.setFechaCreacion(LocalDateTime.now());
                alert.setEstado("ACTIVA");
                savedAlert = alertRepository.save(alert);
                log.info("Alerta creada para: {}", alert.getPlaca());
            }

            // Notificar en tiempo real por WebSocket
            notificationService.notifyAlert(AlertDTO.fromEntity(savedAlert));

        } catch (Exception e) {
            log.error("❌ Error guardando alerta para: {}", alert.getPlaca(), e);
        }
    }

    /**
     * Calcula alertas de cambio de aceite para TODAS las máquinas, vehículos y motos activos
     * Se ejecuta periódicamente o cuando se refresca el consolidado
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void calculateAllOilChangeAlerts() {
        try {
            log.info("🔄 [OIL_ALERTS] Iniciando cálculo de alertas de cambio de aceite para TODOS los activos...");

            // Limpiar alertas GREEN y BLUE antiguas (solo mantener YELLOW y RED)
            try {
                var greenAndBlueAlerts = alertRepository.findByTipoAlertaAndColorEstadoIn(
                    "CAMBIO_ACEITE",
                    java.util.Arrays.asList("GREEN", "BLUE")
                );
                for (var alert : greenAndBlueAlerts) {
                    alert.setEstado("INACTIVA");
                    alertRepository.save(alert);
                }
                if (!greenAndBlueAlerts.isEmpty()) {
                    log.info("🧹 Limpiadas {} alertas GREEN/BLUE antiguas", greenAndBlueAlerts.size());
                }
            } catch (Exception e) {
                log.debug("⚠️ No se pudo limpiar alertas antiguas: {}", e.getMessage());
            }

            // Obtener todos los vehículos activos
            var allVehicles = vehicleRepository.findAll();
            int vehicleCount = 0;
            for (VehicleEntity vehicle : allVehicles) {
                if (vehicle.getActivo() != null && vehicle.getActivo()) { // activo = true
                    try {
                        String placaNorm = vehicle.getPlaca().toUpperCase().trim();
                        checkOilChangeAlertForVehicleByKm(placaNorm);
                        vehicleCount++;
                    } catch (Exception e) {
                        log.warn("⚠️ Error calculando alerta para vehículo: {}", vehicle.getPlaca(), e);
                    }
                }
            }

            // Obtener todas las máquinas activas
            var allMachines = machineRepository.findAll();
            int machineCount = 0;
            for (MachineEntity machine : allMachines) {
                if (machine.getStatus() != null && machine.getStatus()) { // status = true = activo
                    try {
                        checkOilChangeAlertForMachineByHours(machine.getId(), machine.getName(), true);
                        checkOilChangeAlertForMachineByHours(machine.getId(), machine.getName(), false);
                        machineCount++;
                    } catch (Exception e) {
                        log.warn("⚠️ Error calculando alerta para máquina: {}", machine.getName(), e);
                    }
                }
            }

            log.info("✅ [OIL_ALERTS] Alertas calculadas - Vehículos: {}, Máquinas: {}", vehicleCount, machineCount);

        } catch (Exception e) {
            log.error("❌ [OIL_ALERTS] Error en cálculo masivo de alertas", e);
        }
    }
}
