package com.app.usochicamochabackend.notifications.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertSchedulerService {

    private final PreventiveAlertCalculationService preventiveAlertService;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("🚀 AlertSchedulerService: recalculando alertas al iniciar servidor...");
        try {
            preventiveAlertService.calculateAndEmitAlerts();
            log.info("✅ AlertSchedulerService: alertas actualizadas al inicio");
        } catch (Exception e) {
            log.error("❌ AlertSchedulerService: error en cálculo inicial: {}", e.getMessage(), e);
        }
    }

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
