package com.app.usochicamochabackend.notifications.application.service;

import com.app.usochicamochabackend.shared.dto.OilChangeAlertPayload;
import com.app.usochicamochabackend.vehicle.application.dto.VehicleMonitoringDTO;
import com.app.usochicamochabackend.moto.application.dto.MotoMonitoringDTO;
import com.app.usochicamochabackend.notifications.infrastructure.entity.OilChangeAlertEntity;
import com.app.usochicamochabackend.notifications.infrastructure.repository.OilChangeAlertRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Servicio para enviar notificaciones en tiempo real por WebSocket.
 *
 * Utiliza Spring's STOMP message broker para comunicarse con clientes suscritos.
 */
@Service
@RequiredArgsConstructor
public class NotificationWebSocketService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationWebSocketService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final OilChangeAlertRepository alertRepository;

    /**
     * Envía alerta de cambio de aceite a través de WebSocket y guarda en BD.
     *
     * Topic: /topic/oil-change-alerts
     * Clientes suscritos recibirán la alerta inmediatamente.
     */
    public void broadcastOilChangeAlert(VehicleMonitoringDTO vehicle) {
        try {
            OilChangeAlertPayload payload = OilChangeAlertPayload.fromVehicleMonitoring(vehicle);
            if (payload == null) {
                logger.warn("No se puede crear payload para vehículo: {}", vehicle.placa());
                return;
            }

            // 1. Enviar por WebSocket
            messagingTemplate.convertAndSend("/topic/oil-change-alerts", payload);

            // 2. Guardar en BD para auditoría
            saveAlertToDatabase(payload, vehicle.maintenance().estado());

            logger.info("Alerta enviada (WebSocket + BD): {} | {} | {}%",
                vehicle.placa(),
                vehicle.maintenance().alertColor(),
                vehicle.maintenance().percentageUsed());

        } catch (Exception e) {
            logger.error("Error enviando alerta de cambio de aceite (vehículo): {}", vehicle.placa(), e);
        }
    }

    /**
     * Envía alerta de cambio de aceite para motocicleta a través de WebSocket y guarda en BD.
     */
    public void broadcastOilChangeAlert(MotoMonitoringDTO moto) {
        try {
            OilChangeAlertPayload payload = OilChangeAlertPayload.fromMotoMonitoring(moto);
            if (payload == null) {
                logger.warn("No se puede crear payload para motocicleta: {}", moto.placa());
                return;
            }

            // 1. Enviar por WebSocket
            messagingTemplate.convertAndSend("/topic/oil-change-alerts", payload);

            // 2. Guardar en BD para auditoría
            saveAlertToDatabase(payload, moto.oil().estado());

            logger.info("Alerta enviada (WebSocket + BD): {} | {} | {}%",
                moto.placa(),
                moto.oil().alertColor(),
                moto.oil().percentageUsed());

        } catch (Exception e) {
            logger.error("Error enviando alerta de cambio de aceite (motocicleta): {}", moto.placa(), e);
        }
    }

    /**
     * Guarda alerta en BD para auditoría y análisis.
     */
    private void saveAlertToDatabase(OilChangeAlertPayload payload, String estado) {
        try {
            OilChangeAlertEntity alert = new OilChangeAlertEntity();
            alert.setPlaca(payload.placa());
            alert.setTipoMaquinaria(payload.tipoMaquinaria());
            alert.setAlertColor(payload.alertColor());
            alert.setAlertStatus(payload.alertStatus());
            alert.setPercentageUsed(payload.percentageUsed());
            alert.setAlertMessage(payload.alertMessage());
            alert.setNotificationSentAt(payload.timestamp());

            alertRepository.save(alert);
        } catch (Exception e) {
            logger.error("Error guardando alerta en BD para {}: {}", payload.placa(), e.getMessage());
            // No fallar: WebSocket ya se envió, BD es secundaria
        }
    }

    /**
     * Envía un mensaje de notificación general.
     *
     * Topic: /topic/notifications
     */
    public void broadcastGeneralNotification(String message) {
        try {
            messagingTemplate.convertAndSend("/topic/notifications", message);
            logger.debug("Notificación general enviada: {}", message);
        } catch (Exception e) {
            logger.error("Error enviando notificación general", e);
        }
    }
}
