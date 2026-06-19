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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
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
    @Transactional(readOnly = false)
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
        log.info("🔔 Chequear documentos para máquina: {}", machineName);

        try {
            LocalDate threshold = LocalDate.now().plusDays(ALERT_DOCUMENT_DAYS);
            LocalDate today = LocalDate.now();

            log.info("🔔 Umbral de alerta: {}", threshold);

            // Verificar SOAT
            try {
                LocalDate soatDate = (LocalDate) machine.getClass().getMethod("getSoat").invoke(machine);
                log.info("🔔 SOAT para {}: {}", machineName, soatDate);
                if (soatDate != null && !soatDate.isAfter(threshold) && !soatDate.isBefore(today)) {
                    log.info("🔔 Creando alerta SOAT para {}", machineName);
                    createAlert(
                        machineName,
                        "DOCUMENTO_SOAT",
                        "Documento SOAT próximo a vencer",
                        soatDate,
                        null,
                        "MAQUINARIA"
                    );
                } else if (soatDate != null) {
                    log.info("🔔 SOAT {} no necesita alerta (vencimiento: {})", machineName, soatDate);
                }
            } catch (Exception e) {
                log.warn("❌ Error al obtener SOAT para {}: {}", machineName, e.getMessage());
            }

            // Verificar RUNT
            try {
                LocalDate runtDate = (LocalDate) machine.getClass().getMethod("getRunt").invoke(machine);
                log.info("🔔 RUNT para {}: {}", machineName, runtDate);
                if (runtDate != null && !runtDate.isAfter(threshold) && !runtDate.isBefore(today)) {
                    log.info("🔔 Creando alerta RUNT para {}", machineName);
                    createAlert(
                        machineName,
                        "DOCUMENTO_RUNT",
                        "Documento RUNT próximo a vencer",
                        runtDate,
                        null,
                        "MAQUINARIA"
                    );
                } else if (runtDate != null) {
                    log.info("🔔 RUNT {} no necesita alerta (vencimiento: {})", machineName, runtDate);
                }
            } catch (Exception e) {
                log.warn("❌ Error al obtener RUNT para {}: {}", machineName, e.getMessage());
            }
        } catch (Exception e) {
            log.error("❌ Error critical checking document alerts for machine {}: {}", machineName, e.getMessage(), e);
        }
    }

    /**
     * PASO 1 (ADR): Chequear documentos próximos a vencer para VEHÍCULOS/MOTOS
     * Consulta: SELECT * FROM documentacion_y_elementos
     *           WHERE id_vehiculo = (SELECT id_vehiculo FROM vehiculos WHERE placa = ?)
     *           AND activo = true AND fecha_vencimiento <= HOY + 30 DÍAS
     */
    private void checkDocumentAlertsForVehicle(String placa) {
        log.info("🔔 Chequear documentos para vehículo/moto: {}", placa);

        // Obtener el vehículo para acceder a su ID
        var vehiculo = vehicleRepository.findByPlaca(placa);
        if (vehiculo.isEmpty()) {
            log.info("❌ Vehículo no encontrado para placa: {}", placa);
            return;
        }

        Integer idVehiculo = vehiculo.get().getIdVehiculo();
        String tipoVehiculo = vehiculo.get().getTipoVehiculo() != null ? vehiculo.get().getTipoVehiculo().getNombreTipo() : "VEHICULO";
        log.info("🔔 ID vehículo obtenido: {} (tipo: {})", idVehiculo, tipoVehiculo);

        LocalDate threshold = LocalDate.now().plusDays(ALERT_DOCUMENT_DAYS);
        LocalDate today = LocalDate.now();
        YearMonth mesActual = YearMonth.from(today);
        log.info("🔔 Búsqueda: hoy={}, threshold={}", today, threshold);

        var allDocs = documentacionRepository.findAll();
        log.info("🔔 Total documentos en BD: {}", allDocs.size());

        var docs = allDocs.stream()
            .peek(doc -> log.debug("🔍 Documento: id={}, idVehiculo={}, tipoDocumento={}, vencimiento={}, activo={}",
                doc.getIdDocumento(), doc.getIdVehiculo(), doc.getTipoDocumento(), doc.getFechaVencimiento(), doc.getActivo()))
            .filter(doc -> idVehiculo.equals(doc.getIdVehiculo()))
            .filter(doc -> Boolean.TRUE.equals(doc.getActivo()))
            .filter(doc -> doc.getFechaVencimiento() != null)
            .toList();

        log.info("🔔 Documentos activos para {}: {}", placa, docs.size());

        // Crear alertas por cada documento
        docs.forEach(doc -> {
            String tipoDoc = doc.getTipoDocumento();
            LocalDate vencimiento = doc.getFechaVencimiento();

            if ("EXTINTOR".equals(tipoDoc)) {
                // EXTINTOR: Lógica por MES
                YearMonth mesVencimiento = YearMonth.from(vencimiento);
                YearMonth mesAnterior = mesVencimiento.minusMonths(1);

                if (mesActual.equals(mesAnterior)) {
                    // Próximo a vencer (1 mes de anticipación)
                    log.info("🔔 EXTINTOR próximo a vencer: {} (vence: {})", tipoDoc, vencimiento);
                    createAlert(
                        placa,
                        "DOCUMENTO_" + tipoDoc.toUpperCase(),
                        "Documento EXTINTOR próximo a vencer",
                        vencimiento,
                        doc.getIdDocumento(),
                        tipoVehiculo
                    );
                } else if (mesActual.isAfter(mesAnterior)) {
                    // Vencido (estamos en mes de vencimiento o después)
                    log.info("🔔 EXTINTOR VENCIDO: {} (vence: {})", tipoDoc, vencimiento);
                    createAlert(
                        placa,
                        "DOCUMENTO_" + tipoDoc.toUpperCase(),
                        "Documento EXTINTOR VENCIDO",
                        vencimiento,
                        doc.getIdDocumento(),
                        tipoVehiculo
                    );
                }
            } else {
                // OTROS DOCUMENTOS (SOAT, TECNO, etc): Lógica por DÍAS
                if (vencimiento.isBefore(today)) {
                    // Vencido (ya pasó la fecha)
                    log.info("🔔 Documento VENCIDO: {} (vence: {})", tipoDoc, vencimiento);
                    createAlert(
                        placa,
                        "DOCUMENTO_" + tipoDoc.toUpperCase(),
                        String.format("Documento %s VENCIDO", tipoDoc),
                        vencimiento,
                        doc.getIdDocumento(),
                        tipoVehiculo
                    );
                } else if (!vencimiento.isAfter(threshold)) {
                    // Próximo a vencer (dentro de 30 días)
                    log.info("✅ Documento próximo a vencer: {} (vence: {})", tipoDoc, vencimiento);
                    createAlert(
                        placa,
                        "DOCUMENTO_" + tipoDoc.toUpperCase(),
                        String.format("Documento %s próximo a vencer", tipoDoc),
                        vencimiento,
                        doc.getIdDocumento(),
                        tipoVehiculo
                    );
                }
            }
        });
    }

    /**
     * Crear alerta de licencia de conducción próxima a vencer
     */
    public void createLicenseAlert(String username, String licenseCategory, LocalDate licenseExpiry) {
        try {
            if (licenseExpiry == null) {
                log.debug("No license expiry date for user: {}", username);
                return;
            }

            log.info("🔔 Creando alerta de licencia para usuario: {} (vence: {})", username, licenseExpiry);

            // Validar que no exista alerta ACTIVA del mismo tipo
            var existingAlert = alertRepository.findTopByPlacaAndTipoAlertaAndEstadoOrderByFechaCreacionDesc(
                username,
                "DOCUMENTO_LICENCIA",
                "ACTIVA"
            );

            if (existingAlert.isPresent()) {
                log.info("⚠️ Alerta de licencia ya existe para usuario: {}", username);
                return;
            }

            String descripcion = String.format("🪪 Licencia de conducción (categoría %s) próxima a vencer",
                licenseCategory != null ? licenseCategory : "N/A");

            AlertEntity alert = AlertEntity.builder()
                .placa(username)
                .tipoAlerta("DOCUMENTO_LICENCIA")
                .estado("ACTIVA")
                .descripcion(descripcion)
                .fechaVencimiento(licenseExpiry)
                .fechaCreacion(LocalDateTime.now())
                .tipoMaquinaria("USUARIO")
                .build();

            alert.calculateColorEstado();
            alertRepository.save(alert);

            log.info("✅ Alerta de licencia creada para usuario: {} (color: {})", username, alert.getColorEstado());

            // Notificar por WebSocket
            notificationService.notifyAlert(AlertDTO.fromEntity(alert));

        } catch (Exception e) {
            log.error("❌ Error creando alerta de licencia: {}", e.getMessage(), e);
        }
    }

    /**
     * PASO 4 (ADR): Crear alerta en BD
     * Valida que no exista alerta activa del mismo tipo/placa antes de crear
     */
    private void createAlert(String placa, String tipo, String descripcion, LocalDate vencimiento) {
        createAlert(placa, tipo, descripcion, vencimiento, null, null);
    }

    private void createAlert(String placa, String tipo, String descripcion, LocalDate vencimiento, Integer documentoId) {
        createAlert(placa, tipo, descripcion, vencimiento, documentoId, null);
    }

    private void createAlert(String placa, String tipo, String descripcion, LocalDate vencimiento, Integer documentoId, String tipoMaquinaria) {
        log.info("🔔 Creando/Actualizando alerta para {} - {} - {} (tipoMaquinaria: {})", placa, tipo, descripcion, tipoMaquinaria);

        // PASO 3 (ADR): Validar que no exista alerta ACTIVA del mismo tipo
        var existingAlert = alertRepository.findTopByPlacaAndTipoAlertaAndEstadoOrderByFechaCreacionDesc(
            placa,
            tipo,
            "ACTIVA"
        );

        AlertEntity alert;
        if (existingAlert.isPresent()) {
            // ACTUALIZAR alerta existente con nueva descripción (puede haber cambiado de "próximo" a "vencido")
            alert = existingAlert.get();
            log.info("📝 Actualizando alerta existente para {} - {}", placa, tipo);
            alert.setDescripcion(descripcion);
            alert.setFechaVencimiento(vencimiento);
        } else {
            // CREAR nueva alerta
            alert = AlertEntity.builder()
                .placa(placa)
                .tipoAlerta(tipo)
                .estado("ACTIVA")
                .descripcion(descripcion)
                .fechaVencimiento(vencimiento)
                .fechaCreacion(LocalDateTime.now())
                .documentoId(documentoId != null ? documentoId.longValue() : null)
                .tipoMaquinaria(tipoMaquinaria != null ? tipoMaquinaria : "DOCUMENTO")
                .build();
            log.info("✨ Creando nueva alerta para {} - {}", placa, tipo);
        }

        alert.calculateColorEstado();

        // PASO 5 (ADR): Persistir en BD
        alertRepository.save(alert);
        log.info("✅ Alerta persistida: {} - {} - {} (tipoMaquinaria: {})", placa, tipo, alert.getColorEstado(), tipoMaquinaria);

        // Notificar inmediatamente por WebSocket
        notificationService.notifyAlert(AlertDTO.fromEntity(alert));
    }
}
