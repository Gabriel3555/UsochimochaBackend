package com.app.usochicamochabackend.notifications.application.service;

import com.app.usochicamochabackend.shared.event.InspectionCompletedEvent;
import com.app.usochicamochabackend.vehicle.application.dto.VehicleMonitoringDTO;
import com.app.usochicamochabackend.vehicle.application.service.VehicleMonitoringService;
import com.app.usochicamochabackend.moto.application.dto.MotoMonitoringDTO;
import com.app.usochicamochabackend.moto.application.service.MotoMonitoringService;
import com.app.usochicamochabackend.machine.application.dto.MachineMonitoringDTO;
import com.app.usochicamochabackend.machine.application.service.MachineMonitoringService;
import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Listener que reacciona a inspecciones completadas.
 *
 * Cuando se registra una inspección:
 * 1. Recalcula el consolidado de aceite
 * 2. Determina si hay nueva alerta (YELLOW o RED)
 * 3. Envía notificación por WebSocket
 * 4. Registra en auditoría
 */
@Service
@RequiredArgsConstructor
public class OilChangeNotificationListener {

    private static final Logger logger = LoggerFactory.getLogger(OilChangeNotificationListener.class);

    private final VehicleMonitoringService vehicleMonitoringService;
    private final MotoMonitoringService motoMonitoringService;
    private final MachineMonitoringService machineMonitoringService;
    private final NotificationWebSocketService notificationWebSocketService;
    private final SaveActionUseCase saveActionUseCase;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void onInspectionCompleted(InspectionCompletedEvent event) {
        try {
            logger.info("Procesando evento de inspección completada: placa={}, tipo={}, km={}",
                event.getPlaca(), event.getTipoMaquinaria(), event.getNewDistance());

            String tipoMaquinaria = event.getTipoMaquinaria();
            String placa = event.getPlaca();

            // 1. Recalcular consolidado según tipo de maquinaria
            if ("VEHICULO".equals(tipoMaquinaria)) {
                processVehicleAlert(placa, event);
            } else if ("MOTOCICLETA".equals(tipoMaquinaria)) {
                processMotoAlert(placa, event);
            } else if ("MAQUINARIA".equals(tipoMaquinaria)) {
                processMachineAlert(event.getMachineId(), event);
            }

            // Registrar en auditoría
            saveActionUseCase.save(
                String.format("Alerta de aceite actualizada en tiempo real: %s | Tipo: %s | Km/Hrs: %d",
                    placa, tipoMaquinaria, event.getNewDistance())
            );

        } catch (Exception e) {
            logger.error("Error procesando InspectionCompletedEvent para placa: {}", event.getPlaca(), e);
            // No lanzar excepción: evitar que el error afecte al servicio de inspecciones
        }
    }

    /**
     * Procesa alerta para un vehículo.
     */
    private void processVehicleAlert(String placa, InspectionCompletedEvent event) {
        try {
            // Obtener consolidado y buscar el vehículo por placa
            var allVehicles = vehicleMonitoringService.getConsolidatedMonitoring();
            var updated = allVehicles.stream()
                .filter(v -> placa.equals(v.placa()))
                .findFirst()
                .orElse(null);

            if (updated == null || updated.maintenance() == null) {
                logger.debug("Vehículo sin historial de aceite: {}", placa);
                return;
            }

            // Validar si debe notificar (solo YELLOW o RED)
            if (shouldNotify(updated.maintenance().alertColor())) {
                // 1. Enviar notificación de alerta específica
                notificationWebSocketService.broadcastOilChangeAlert(updated);

                // 2. Notificar al sistema general que hay actualización
                messagingTemplate.convertAndSend("/topic/notifications/data-update",
                    "oil-change-alert-updated");

                logger.info("Alerta enviada por WebSocket: {} | Color: {} | %: {}",
                    placa,
                    updated.maintenance().alertColor(),
                    updated.maintenance().percentageUsed());
            }

        } catch (Exception e) {
            logger.error("Error procesando alerta para vehículo: {}", placa, e);
        }
    }

    /**
     * Procesa alerta para una motocicleta.
     */
    private void processMotoAlert(String placa, InspectionCompletedEvent event) {
        try {
            // Obtener consolidado y buscar la moto por placa
            var allMotos = motoMonitoringService.getConsolidatedMonitoring();
            var updated = allMotos.stream()
                .filter(m -> placa.equals(m.placa()))
                .findFirst()
                .orElse(null);

            if (updated == null || updated.oil() == null) {
                logger.debug("Motocicleta sin historial de aceite: {}", placa);
                return;
            }

            // Validar si debe notificar (solo YELLOW o RED)
            if (shouldNotify(updated.oil().alertColor())) {
                // 1. Enviar notificación de alerta específica
                notificationWebSocketService.broadcastOilChangeAlert(updated);

                // 2. Notificar al sistema general que hay actualización
                messagingTemplate.convertAndSend("/topic/notifications/data-update",
                    "oil-change-alert-updated");

                logger.info("Alerta enviada por WebSocket: {} | Color: {} | %: {}",
                    placa,
                    updated.oil().alertColor(),
                    updated.oil().percentageUsed());
            }

        } catch (Exception e) {
            logger.error("Error procesando alerta para motocicleta: {}", placa, e);
        }
    }

    /**
     * Procesa alerta para una máquina (motor e hidráulico).
     */
    private void processMachineAlert(Long machineId, InspectionCompletedEvent event) {
        try {
            // Obtener consolidado de la máquina específica
            MachineMonitoringDTO updated = machineMonitoringService.getConsolidateById(machineId);

            if (updated == null) {
                logger.debug("Máquina no encontrada o sin historial de aceite: {}", machineId);
                return;
            }

            // Validar si hay alerta en aceite motor o hidráulico
            boolean motorAlert = shouldNotify(
                updated.motorOilInfo() != null ? updated.motorOilInfo().alertColor() : null
            );
            boolean hydraulicAlert = shouldNotify(
                updated.hydraulicOilInfo() != null ? updated.hydraulicOilInfo().alertColor() : null
            );

            if (motorAlert || hydraulicAlert) {
                // 1. Enviar notificación de alerta específica
                notificationWebSocketService.broadcastOilChangeAlert(updated);

                // 2. Notificar al sistema general que hay actualización
                messagingTemplate.convertAndSend("/topic/notifications/data-update",
                    "oil-change-alert-updated");

                logger.info("Alerta enviada por WebSocket: machineId={} | Motor: {} | Hidráulico: {}",
                    machineId,
                    updated.motorOilInfo() != null ? updated.motorOilInfo().alertColor() : "N/A",
                    updated.hydraulicOilInfo() != null ? updated.hydraulicOilInfo().alertColor() : "N/A");
            }

        } catch (Exception e) {
            logger.error("Error procesando alerta para máquina: {}", machineId, e);
        }
    }

    /**
     * Determina si debe notificar al operario.
     *
     * Solo notificar en casos de alerta:
     * - YELLOW (80-99% uso): "próximo a cambio"
     * - RED (100%+ uso): "cambio URGENTE"
     */
    private boolean shouldNotify(String alertColor) {
        return alertColor != null && ("YELLOW".equals(alertColor) || "RED".equals(alertColor));
    }
}
