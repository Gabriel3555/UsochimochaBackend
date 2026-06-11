package com.app.usochicamochabackend.notifications.application;

import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
import com.app.usochicamochabackend.notifications.application.dto.AlertDTO;
import com.app.usochicamochabackend.notifications.infrastructure.entity.AlertEntity;
import com.app.usochicamochabackend.notifications.infrastructure.repository.AlertRepository;
import com.app.usochicamochabackend.update.infrastructure.entity.OilChangeEntity;
import com.app.usochicamochabackend.update.infrastructure.entity.VehicleOilChangeEntity;
import com.app.usochicamochabackend.update.infrastructure.repository.OilChangeRepository;
import com.app.usochicamochabackend.update.infrastructure.repository.VehicleOilChangeRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.entity.DocumentacionYElementosEntity;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.DocumentacionYElementosRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertCalculationService {

    private final AlertRepository alertRepository;
    private final NotificationService notificationService;
    private final VehicleOilChangeRepository vehicleOilChangeRepository;
    private final OilChangeRepository oilChangeRepository;
    private final DocumentacionYElementosRepository documentacionRepository;
    private final VehicleRepository vehicleRepository;
    private final MachineRepository machineRepository;

    // Umbrales de alerta (definidos en ADR-001)
    private static final int ALERT_OIL_CHANGE_MONTHS = 6;        // 6 meses
    private static final int ALERT_DOCUMENT_DAYS = 30;            // 30 días antes

    /**
     * Calcula TODAS las alertas para una placa específica
     * Detecta si es vehículo/moto o máquina y llama al método correspondiente
     *
     * Basado en datos EXISTENTES en BD (ADR-001)
     */
    public void calculateAlertsForPlate(String placa, String tipoActivo) {
        log.info("🔔 Calculando alertas para placa: {} (tipo: {})", placa, tipoActivo);

        try {
            if ("MÁQUINA".equals(tipoActivo)) {
                calculateAlertsForMachine(placa);
            } else {
                calculateAlertsForVehicle(placa);
            }
            log.info("✅ Alertas calculadas para placa: {}", placa);
        } catch (Exception e) {
            log.error("❌ Error calculando alertas para placa {}: {}", placa, e.getMessage(), e);
        }
    }

    /**
     * Calcula alertas para MÁQUINAS
     * Verifica: cambios de aceite motor/hidráulico y documentos (SOAT, RUNT)
     */
    private void calculateAlertsForMachine(String machineName) {
        log.info("🔔 Calculando alertas para máquina: {}", machineName);

        // Obtener máquina por nombre
        var machine = machineRepository.findByName(machineName);
        if (machine.isEmpty()) {
            log.debug("Machine not found with name: {}", machineName);
            return;
        }

        Long machineId = machine.get().getId();

        // 1. Chequear cambios de aceite motor
        checkOilChangeAlertForMachine(machineId, machineName, true);

        // 2. Chequear cambios de aceite hidráulico
        checkOilChangeAlertForMachine(machineId, machineName, false);

        // 3. Chequear documentos (SOAT, RUNT)
        checkDocumentAlertsForMachine(machine.get(), machineName);
    }

    /**
     * Calcula alertas para VEHÍCULOS y MOTOS
     * Verifica: cambios de aceite y documentos próximos a vencer
     */
    private void calculateAlertsForVehicle(String placa) {
        log.info("🔔 Calculando alertas para vehículo/moto: {}", placa);

        // 1. Chequear alertas de cambio de aceite
        checkOilChangeAlertForVehicle(placa);

        // 2. Chequear alertas de documentos
        checkDocumentAlertsForVehicle(placa);
    }

    /**
     * PASO 1 (ADR): Chequear cambio de aceite para vehículos/motos
     * Consulta: SELECT * FROM vehicle_oil_changes WHERE placa = ? ORDER BY fecha DESC LIMIT 1
     */
    private void checkOilChangeAlertForVehicle(String placa) {
        log.debug("Checking oil change alert for vehicle: {}", placa);

        var allOilChanges = vehicleOilChangeRepository.findAllByPlacaOrderByDateStampDesc(placa);

        if (allOilChanges.isEmpty()) {
            log.debug("No oil change record found for vehicle: {}", placa);
            return;
        }

        var lastOilChange = allOilChanges.get(0); // El primero es el más reciente

        // PASO 2 (ADR): Calcular días transcurridos
        long daysSinceLast = ChronoUnit.DAYS.between(
            lastOilChange.getDateStamp().toLocalDate(),
            LocalDate.now()
        );

        log.debug("Days since last oil change for {}: {}", placa, daysSinceLast);

        // PASO 3 (ADR): Si pasó más de 6 meses, crear alerta
        if (daysSinceLast > (ALERT_OIL_CHANGE_MONTHS * 30)) {
            LocalDate recommendedNextChange = lastOilChange.getDateStamp().toLocalDate()
                .plusMonths(ALERT_OIL_CHANGE_MONTHS);

            createAlert(
                placa,
                "CAMBIO_ACEITE",
                String.format("Cambio de aceite recomendado (última vez hace %d días)", daysSinceLast),
                recommendedNextChange
            );
        }
    }

    /**
     * PASO 2 (ADR): Chequear cambios de aceite para MÁQUINAS
     * Soporta tanto aceite motor como hidráulico
     */
    private void checkOilChangeAlertForMachine(Long machineId, String machineName, boolean isMotorOil) {
        log.debug("Checking {} oil change alert for machine: {}", isMotorOil ? "motor" : "hydraulic", machineName);

        OilChangeEntity lastOilChange = isMotorOil
            ? oilChangeRepository.getLastMotorOilChangeByMachineId(machineId)
            : oilChangeRepository.getLastHydraulicOilChangeByMachineId(machineId);

        if (lastOilChange == null || lastOilChange.getDateStamp() == null) {
            log.debug("No {} oil change record found for machine: {}", isMotorOil ? "motor" : "hydraulic", machineName);
            return;
        }

        long daysSinceLast = ChronoUnit.DAYS.between(
            lastOilChange.getDateStamp().toLocalDate(),
            LocalDate.now()
        );

        log.debug("Days since last {} oil change for {}: {}", isMotorOil ? "motor" : "hydraulic", machineName, daysSinceLast);

        // Si pasó más de 6 meses, crear alerta
        if (daysSinceLast > (ALERT_OIL_CHANGE_MONTHS * 30)) {
            LocalDate recommendedNextChange = lastOilChange.getDateStamp().toLocalDate()
                .plusMonths(ALERT_OIL_CHANGE_MONTHS);

            String oilType = isMotorOil ? "CAMBIO_ACEITE_MOTOR" : "CAMBIO_ACEITE_HIDRAULICO";
            createAlert(
                machineName,
                oilType,
                String.format("Cambio de %s recomendado (última vez hace %d días)",
                    isMotorOil ? "aceite motor" : "aceite hidráulico",
                    daysSinceLast),
                recommendedNextChange
            );
        }
    }

    /**
     * PASO 3 (ADR): Chequear documentos próximos a vencer para MÁQUINAS
     * Verifica: SOAT y RUNT
     */
    private void checkDocumentAlertsForMachine(Object machine, String machineName) {
        log.debug("Checking document alerts for machine: {}", machineName);

        try {
            LocalDate threshold = LocalDate.now().plusDays(ALERT_DOCUMENT_DAYS);
            LocalDate today = LocalDate.now();

            // Verificar SOAT
            LocalDate soatDate = (LocalDate) machine.getClass().getMethod("getSoat").invoke(machine);
            if (soatDate != null && !soatDate.isAfter(threshold) && !soatDate.isBefore(today)) {
                createAlert(
                    machineName,
                    "DOCUMENTO",
                    "Documento SOAT próximo a vencer",
                    soatDate
                );
            }

            // Verificar RUNT
            LocalDate runtDate = (LocalDate) machine.getClass().getMethod("getRunt").invoke(machine);
            if (runtDate != null && !runtDate.isAfter(threshold) && !runtDate.isBefore(today)) {
                createAlert(
                    machineName,
                    "DOCUMENTO",
                    "Documento RUNT próximo a vencer",
                    runtDate
                );
            }
        } catch (Exception e) {
            log.warn("Error checking document alerts for machine {}: {}", machineName, e.getMessage());
        }
    }

    /**
     * PASO 1 (ADR): Chequear documentos próximos a vencer para VEHÍCULOS/MOTOS
     * Consulta: SELECT * FROM documentacion_y_elementos
     *           WHERE id_vehiculo = (SELECT id_vehiculo FROM vehiculos WHERE placa = ?)
     *           AND activo = true AND fecha_vencimiento <= HOY + 30 DÍAS
     */
    private void checkDocumentAlertsForVehicle(String placa) {
        log.debug("Checking document alerts for: {}", placa);

        // Obtener el vehículo para acceder a su ID
        var vehiculo = vehicleRepository.findByPlaca(placa);
        if (vehiculo.isEmpty()) {
            log.debug("Vehicle not found for placa: {}", placa);
            return;
        }

        Integer idVehiculo = vehiculo.get().getIdVehiculo();
        LocalDate threshold = LocalDate.now().plusDays(ALERT_DOCUMENT_DAYS);
        LocalDate today = LocalDate.now();

        var docsExpiring = documentacionRepository.findAll()
            .stream()
            .filter(doc -> idVehiculo.equals(doc.getIdVehiculo()))
            .filter(doc -> Boolean.TRUE.equals(doc.getActivo()))
            .filter(doc -> doc.getFechaVencimiento() != null)
            // PASO 2 (ADR): Filtrar solo documentos que vencen en < 30 días
            .filter(doc -> !doc.getFechaVencimiento().isAfter(threshold))
            .filter(doc -> !doc.getFechaVencimiento().isBefore(today))
            .toList();

        log.debug("Found {} documents expiring for {}", docsExpiring.size(), placa);

        // PASO 3 (ADR): Crear alerta por cada documento que vence
        docsExpiring.forEach(doc -> {
            createAlert(
                placa,
                "DOCUMENTO",
                String.format("Documento %s próximo a vencer", doc.getTipoDocumento()),
                doc.getFechaVencimiento(),
                doc.getIdDocumento()
            );
        });
    }

    /**
     * PASO 4 (ADR): Crear alerta en BD
     * Valida que no exista alerta activa del mismo tipo/placa antes de crear
     */
    private void createAlert(String placa, String tipo, String descripcion, LocalDate vencimiento) {
        createAlert(placa, tipo, descripcion, vencimiento, null);
    }

    private void createAlert(String placa, String tipo, String descripcion, LocalDate vencimiento, Integer documentoId) {
        log.debug("Creating alert for {} - {} - {}", placa, tipo, descripcion);

        // PASO 3 (ADR): Validar que no exista alerta ACTIVA del mismo tipo
        var existingAlert = alertRepository.findTopByPlacaAndTipoAlertaAndEstadoOrderByFechaCreacionDesc(
            placa,
            tipo,
            "ACTIVE"
        );

        if (existingAlert.isPresent()) {
            log.debug("Alert already exists for {} - {}", placa, tipo);
            return; // No crear si ya existe activa
        }

        // PASO 4 (ADR): Calcular color_estado
        AlertEntity alert = AlertEntity.builder()
            .placa(placa)
            .tipoAlerta(tipo)
            .estado("ACTIVE")
            .descripcion(descripcion)
            .fechaVencimiento(vencimiento)
            .fechaCreacion(LocalDateTime.now())
            .documentoId(documentoId != null ? documentoId.longValue() : null)
            .build();

        alert.calculateColorEstado();

        // PASO 5 (ADR): Persistir en BD
        alertRepository.save(alert);
        log.info("✅ Alert created: {} - {} - {}", placa, tipo, alert.getColorEstado());

        // Notificar inmediatamente por WebSocket (sin romper lo existente)
        notificationService.notifyAlert(AlertDTO.fromEntity(alert));
    }
}
