package com.app.usochicamochabackend.notifications.application;

import com.app.usochicamochabackend.auth.infrastructure.repository.UserRepositoryJpa;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.DocumentacionYElementosRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertSchedulerService {

    private final NotificationService notificationService;
    private final DocumentacionYElementosRepository documentacionRepository;
    private final UserRepositoryJpa userRepository;
    private final AlertCalculationService alertCalculationService;
    private final VehicleRepository vehicleRepository;
    private final MachineRepository machineRepository;

    /**
     * Runs every day at 07:00 to check for expiring vehicle documents and driver licenses.
     * Also calculates preventive alerts based on oil changes, documents, and licenses.
     * Broadcasts WebSocket alerts to connected admin users.
     */
    @Scheduled(cron = "0 0 7 * * *")
    public void checkExpiringDocuments() {
        log.info("🔔 AlertSchedulerService: iniciando verificación diaria de vencimientos y alertas...");

        LocalDate threshold = LocalDate.now().plusDays(30);

        // MANTENER: Count active vehicle documents expiring within the next 30 days
        List<?> expiringDocs = documentacionRepository.findAll().stream()
                .filter(doc -> Boolean.TRUE.equals(doc.getActivo())
                        && doc.getFechaVencimiento() != null
                        && !doc.getFechaVencimiento().isAfter(threshold)
                        && !doc.getFechaVencimiento().isBefore(LocalDate.now()))
                .toList();

        // Crear alertas para licencias de usuarios próximas a vencer
        var usersWithLicense = userRepository.findAll().stream()
                .filter(user -> Boolean.TRUE.equals(user.getStatus())
                        && user.getLicenseExpiry() != null
                        && !user.getLicenseExpiry().isAfter(threshold)
                        && !user.getLicenseExpiry().isBefore(LocalDate.now()))
                .toList();

        log.info("🔔 Usuarios con licencia próxima a vencer: {}", usersWithLicense.size());

        List<?> expiringLicenses = usersWithLicense.stream()
                .peek(user -> {
                    com.app.usochicamochabackend.auth.infrastructure.entity.UserEntity userEntity =
                        (com.app.usochicamochabackend.auth.infrastructure.entity.UserEntity) user;
                    log.info("🔔 Creando alerta de licencia para usuario: {} (vence: {})",
                        userEntity.getUsername(), userEntity.getLicenseExpiry());
                    alertCalculationService.createLicenseAlert(
                        userEntity.getUsername(),
                        userEntity.getLicenseCategory(),
                        userEntity.getLicenseExpiry()
                    );
                })
                .toList();

        String summary = String.format(
                "Revisión diaria de vencimientos completada - %s | Documentos próximos a vencer: %d | Licencias próximas a vencer: %d",
                LocalDate.now(),
                expiringDocs.size(),
                expiringLicenses.size()
        );

        try {
            log.info("🔔 Calculando alertas preventivas para todos los activos...");
            calculatePreventiveAlerts();
            log.info("✅ Alertas preventivas calculadas exitosamente");
        } catch (Exception e) {
            log.error("❌ Error calculando alertas preventivas: {}", e.getMessage(), e);
        }

        log.info("AlertSchedulerService: {}", summary);
        notificationService.notifyDocumentExpiry(summary);
    }

    /**
     * NUEVO: Calcula alertas preventivas para todos los vehículos, motos y máquinas
     * Consulta todas las placas activas y calcula sus alertas
     * Método público para ser llamado cuando se conecta un cliente WebSocket
     */
    public void calculatePreventiveAlerts() {
        log.info("🔔 === INICIANDO calculatePreventiveAlerts ===");
        Set<String> allPlacas = new HashSet<>();

        // Obtener todas las placas de vehículos activos (incluyendo motos)
        vehicleRepository.findAllActiveVehiclesWithDocuments().stream()
                .forEach(v -> allPlacas.add(v.getPlaca()));

        // Obtener todas las máquinas activas
        machineRepository.findAll().stream()
                .filter(m -> Boolean.TRUE.equals(m.getStatus()))
                .forEach(m -> allPlacas.add(m.getName())); // Máquinas usan nombre como ID

        log.info("🔔 Calculando alertas para {} placas activas: {}", allPlacas.size(), allPlacas);

        // Para cada placa, calcular sus alertas
        for (String placa : allPlacas) {
            try {
                String tipoActivo = determineAssetType(placa);
                log.info("🔔 Calculando alertas para placa: {} (tipo: {})", placa, tipoActivo);
                alertCalculationService.calculateAlertsForPlate(placa, tipoActivo);
            } catch (Exception e) {
                log.warn("⚠️ Error calculando alertas para placa {}: {}", placa, e.getMessage(), e);
            }
        }

        // Crear alertas para licencias de usuarios próximas a vencer
        LocalDate threshold = LocalDate.now().plusDays(30);
        var usersWithLicense = userRepository.findAll().stream()
                .filter(user -> Boolean.TRUE.equals(user.getStatus())
                        && user.getLicenseExpiry() != null
                        && !user.getLicenseExpiry().isAfter(threshold)
                        && !user.getLicenseExpiry().isBefore(LocalDate.now()))
                .toList();

        log.info("🔔 Usuarios con licencia próxima a vencer: {}", usersWithLicense.size());

        usersWithLicense.forEach(user -> {
            log.info("🔔 Creando alerta de licencia para usuario: {} (vence: {})",
                user.getUsername(), user.getLicenseExpiry());
            alertCalculationService.createLicenseAlert(
                user.getUsername(),
                user.getLicenseCategory(),
                user.getLicenseExpiry()
            );
        });
    }

    /**
     * Determina el tipo de activo (VEHÍCULO, MÁQUINA) basado en la placa
     * Las motos se consideran VEHÍCULO (están en la tabla vehiculos)
     */
    private String determineAssetType(String placa) {
        // Si existe en vehículos (incluyendo motos)
        if (vehicleRepository.findByPlaca(placa).isPresent()) {
            return "VEHÍCULO";
        }
        // Si no existe en vehículos, es máquina
        return "MÁQUINA";
    }
}
