package com.app.usochicamochabackend.notifications.application;

import com.app.usochicamochabackend.auth.infrastructure.repository.UserRepositoryJpa;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.DocumentacionYElementosRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertSchedulerService {

    private final NotificationService notificationService;
    private final DocumentacionYElementosRepository documentacionRepository;
    private final UserRepositoryJpa userRepository;

    /**
     * Runs every day at 07:00 to check for expiring vehicle documents and driver licenses.
     * Broadcasts a WebSocket alert so connected admin users are informed.
     */
    @Scheduled(cron = "0 0 7 * * *")
    public void checkExpiringDocuments() {
        log.info("AlertSchedulerService: checking expiring documents and licenses...");

        LocalDate threshold = LocalDate.now().plusDays(30);

        // Count active vehicle documents expiring within the next 30 days
        List<?> expiringDocs = documentacionRepository.findAll().stream()
                .filter(doc -> Boolean.TRUE.equals(doc.getActivo())
                        && doc.getFechaVencimiento() != null
                        && !doc.getFechaVencimiento().isAfter(threshold)
                        && !doc.getFechaVencimiento().isBefore(LocalDate.now()))
                .toList();

        // Count users (operators/drivers) whose license expires within 30 days
        List<?> expiringLicenses = userRepository.findAll().stream()
                .filter(user -> Boolean.TRUE.equals(user.getStatus())
                        && user.getLicenseExpiry() != null
                        && !user.getLicenseExpiry().isAfter(threshold)
                        && !user.getLicenseExpiry().isBefore(LocalDate.now()))
                .toList();

        String summary = String.format(
                "Revisión diaria de vencimientos completada - %s | Documentos próximos a vencer: %d | Licencias próximas a vencer: %d",
                LocalDate.now(),
                expiringDocs.size(),
                expiringLicenses.size()
        );

        log.info("AlertSchedulerService: {}", summary);
        notificationService.notifyDocumentExpiry(summary);
    }
}
