package com.app.usochicamochabackend.notifications.infrastructure.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handle ping messages from connected clients to keep the connection alive
     */
    @MessageMapping("/ping")
    public void handlePing(@Payload String message) {
        log.debug("Received ping from client: {}", message);
        messagingTemplate.convertAndSend("/topic/notifications/ping-response", 
            "pong");
    }

    /**
     * Handle client subscription to notifications
     */
    @MessageMapping("/subscribe")
    public void handleSubscription(@Payload String subscription) {
        log.debug("Client subscribed to: {}", subscription);
        // Send confirmation of subscription
        messagingTemplate.convertAndSend("/topic/notifications/subscription-confirm", 
            "subscribed");
    }

    /**
     * Handle client unsubscription from notifications
     */
    @MessageMapping("/unsubscribe")
    public void handleUnsubscription(@Payload String unsubscription) {
        log.debug("Client unsubscribed from: {}", unsubscription);
        // Send confirmation of unsubscription
        messagingTemplate.convertAndSend("/topic/notifications/subscription-confirm", 
            "unsubscribed");
    }

    /**
     * Send inspection notification to all connected clients
     */
    public void broadcastInspection(String inspectionData) {
        log.debug("Broadcasting inspection data: {}", inspectionData);
        messagingTemplate.convertAndSend("/topic/notifications/inspection", inspectionData);
    }

    /**
     * Send oil change notification to all connected clients
     */
    public void broadcastOilChange(String oilChangeData) {
        log.debug("Broadcasting oil change data: {}", oilChangeData);
        messagingTemplate.convertAndSend("/topic/oil-change-alerts", oilChangeData);
    }

    /**
     * Send specific notification to a particular user
     */
    public void sendToUser(String username, String notification) {
        log.debug("Sending notification to user {}: {}", username, notification);
        messagingTemplate.convertAndSend("/user/" + username + "/notifications", notification);
    }

    /**
     * Send connection status to connected clients
     */
    public void sendConnectionStatus(String status) {
        log.debug("Sending connection status: {}", status);
        messagingTemplate.convertAndSend("/topic/notifications/connection", status);
    }

    /**
     * Send SOAT/RUNT notification to all connected clients
     */
    public void broadcastSoatRuntNotification(String soatRuntData) {
        log.debug("Broadcasting SOAT/RUNT notification: {}", soatRuntData);
        messagingTemplate.convertAndSend("/topic/notifications/soat-runt", soatRuntData);
    }

    /**
     * Send SOAT/RUNT stream status to connected clients
     */
    public void broadcastSoatRuntStreamStatus(String status) {
        log.debug("Broadcasting SOAT/RUNT stream status: {}", status);
        messagingTemplate.convertAndSend("/topic/notifications/soat-runt-stream", status);
    }

    /**
     * Send specific SOAT/RUNT notification to a particular user
     */
    public void sendSoatRuntToUser(String username, String soatRuntData) {
        log.debug("Sending SOAT/RUNT notification to user {}: {}", username, soatRuntData);
        messagingTemplate.convertAndSend("/user/" + username + "/notifications/soat-runt", soatRuntData);
    }

    public void broadcastFuelAnomaly(String anomalyData) {
        log.debug("Broadcasting fuel anomaly: {}", anomalyData);
        messagingTemplate.convertAndSend("/topic/alerts/fuel-anomaly", anomalyData);
    }

    /**
     * Broadcast a data-update event to all connected clients (e.g. "vehicle-inspections-updated")
     */
    public void broadcastDataUpdate(String message) {
        log.debug("Broadcasting data update: {}", message);
        messagingTemplate.convertAndSend("/topic/notifications/data-update", message);
    }
}