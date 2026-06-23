package com.app.usochicamochabackend.notifications.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertSchedulerService {

    private final PreventiveAlertCalculationService preventiveAlertService;

    /**
     * Scheduler ejecuta diariamente a las 07:00
     * Calcula TODAS las alertas preventivas del sistema (FASE 2)
     */
    @Scheduled(cron = "0 0 7 * * *")
    public void schedulePreventiveAlerts() {
        log.info("🔔 AlertSchedulerService: iniciando scheduler de alertas preventivas...");

        try {
            preventiveAlertService.calculateAndEmitAlerts();
            log.info("✅ AlertSchedulerService: scheduler completado exitosamente");
        } catch (Exception e) {
            log.error("❌ AlertSchedulerService: error en scheduler: {}", e.getMessage(), e);
        }
    }

}
