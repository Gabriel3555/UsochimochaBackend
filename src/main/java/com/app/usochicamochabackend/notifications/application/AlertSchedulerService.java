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
        try {
            preventiveAlertService.calculateAndEmitAlerts();
        } catch (Exception e) {
            log.error("❌ AlertSchedulerService: error en cálculo inicial: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 5 * * *")
    public void schedulePreventiveAlerts() {
        try {
            preventiveAlertService.calculateAndEmitAlerts();
        } catch (Exception e) {
            log.error("❌ AlertSchedulerService: error en scheduler: {}", e.getMessage(), e);
        }
    }

}
