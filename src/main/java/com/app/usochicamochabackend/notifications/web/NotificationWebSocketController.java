    package com.app.usochicamochabackend.notifications.web;

import com.app.usochicamochabackend.notifications.application.NotificationService;
import com.app.usochicamochabackend.notifications.application.AlertSchedulerService;
import com.app.usochicamochabackend.notifications.application.service.NotificationWebSocketService;
import com.app.usochicamochabackend.review.application.dto.InspectionFormResponse;
import com.app.usochicamochabackend.notifications.infrastructure.websocket.NotificationWebSocketHandler;
import com.app.usochicamochabackend.vehicle.application.service.VehicleMonitoringService;
import com.app.usochicamochabackend.moto.application.service.MotoMonitoringService;
import com.app.usochicamochabackend.machine.application.service.MachineMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.messaging.SessionConnectEvent;

import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/ws/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketController {

    private final NotificationService notificationService;
    private final NotificationWebSocketHandler webSocketHandler;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationWebSocketService notificationWebSocketService;
    private final AlertSchedulerService alertSchedulerService;
    private final VehicleMonitoringService vehicleMonitoringService;
    private final MotoMonitoringService motoMonitoringService;
    private final MachineMonitoringService machineMonitoringService;

    private final ConcurrentHashMap<String, String> activeConnections = new ConcurrentHashMap<>();

    /**
     * Connection tracking endpoints
     */
    @MessageMapping("/connect")
    public void handleConnection(@Header("simpSessionId") String sessionId, @Header("user") String user) {
        String userKey = user != null ? user : sessionId;
        activeConnections.put(sessionId, userKey);
        log.info("WebSocket connected - Session: {}, User: {}", sessionId, userKey);
        
        // Send connection confirmation
        messagingTemplate.convertAndSend("/topic/notifications/connection", 
            "connected:" + userKey);
    }

    @MessageMapping("/disconnect")
    public void handleDisconnection(@Header("simpSessionId") String sessionId) {
        String user = activeConnections.remove(sessionId);
        log.info("WebSocket disconnected - Session: {}, User: {}", sessionId, user);

        // Send disconnection notification
        messagingTemplate.convertAndSend("/topic/notifications/connection",
            "disconnected:" + user);
    }

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        log.info("🔌 [WEBSOCKET] Cliente conectado - enviando snapshot de alertas...");

        new Thread(() -> {
            try {
                Thread.sleep(1000);
                log.info("📸 [SNAPSHOT] Calculando y enviando TODAS las alertas...");
                sendOilChangeAlertsSnapshot();

                log.info("✅ [SNAPSHOT] Todas las alertas enviadas");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Error en thread de snapshot", e);
            }
        }).start();
    }

    private void sendOilChangeAlertsSnapshot() {
        try {
            log.info("📸 [SNAPSHOT] Iniciando cálculo y envío de alertas de cambio de aceite...");

            // Calcular alertas para todos los activos
            alertSchedulerService.calculatePreventiveAlerts();

            log.info("✅ [SNAPSHOT] Snapshot de alertas de cambio de aceite completado");
        } catch (Exception e) {
            log.error("❌ [SNAPSHOT] Error enviando snapshot de alertas", e);
        }
    }

    /**
     * WebSocket notification endpoints
     */
    @PostMapping("/inspection")
    public ResponseEntity<String> sendInspection(@RequestBody String inspectionData) {
        log.debug("Sending WebSocket inspection: {}", inspectionData);
        notificationService.notifyInspection(inspectionData);
        
        return ResponseEntity.ok("WebSocket inspection notification sent");
    }

    @PostMapping("/oil-change")
    public ResponseEntity<String> sendOilChange(@RequestBody String oilChangeData) {
        log.debug("Sending WebSocket oil change: {}", oilChangeData);
        notificationService.notifyOilChange(oilChangeData);
        
        return ResponseEntity.ok("WebSocket oil change notification sent");
    }

    @PostMapping("/user")
    public ResponseEntity<String> sendUserNotification(
            @RequestParam String username, 
            @RequestParam String notification) {
        
        log.debug("Sending WebSocket user notification to {}: {}", username, notification);
        notificationService.notifyUser(username, notification);
        
        return ResponseEntity.ok("WebSocket user notification sent to " + username);
    }

    /**
     * Health check endpoints
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("WebSocket notifications healthy");
    }

    @GetMapping("/connections")
    public ResponseEntity<ConcurrentHashMap<String, String>> getActiveConnections() {
        return ResponseEntity.ok(new ConcurrentHashMap<>(activeConnections));
    }

    @GetMapping("/stats")
    public ResponseEntity<ConcurrentHashMap<String, Long>> getNotificationStats() {
        return ResponseEntity.ok(notificationService.getNotificationStats());
    }

    @PostMapping("/stats/reset")
    public ResponseEntity<String> resetStats() {
        notificationService.resetNotificationStats();
        return ResponseEntity.ok("Notification statistics reset");
    }

    /**
     * SOAT/RUNT WebSocket notification endpoints
     */
    @PostMapping("/soat-runt")
    public ResponseEntity<String> sendSoatRunt(@RequestBody String soatRuntData) {
        log.debug("Sending SOAT/RUNT WebSocket notification: {}", soatRuntData);
        notificationService.notifySoatRunt(soatRuntData);
        
        return ResponseEntity.ok("SOAT/RUNT WebSocket notification sent");
    }

    @PostMapping("/soat-runt/stream-status")
    public ResponseEntity<String> sendSoatRuntStreamStatus(@RequestParam String status) {
        log.debug("Sending SOAT/RUNT stream status WebSocket notification: {}", status);
        notificationService.notifySoatRuntStreamStatus(status);
        
        return ResponseEntity.ok("SOAT/RUNT stream status WebSocket notification sent");
    }

    @PostMapping("/soat-runt/user")
    public ResponseEntity<String> sendSoatRuntUser(
            @RequestParam String username,
            @RequestBody String soatRuntData) {
        
        log.debug("Sending SOAT/RUNT WebSocket notification to user {}: {}", username, soatRuntData);
        notificationService.notifySoatRuntUser(username, soatRuntData);
        
        return ResponseEntity.ok("SOAT/RUNT WebSocket user notification sent to " + username);
    }

    // Legacy SSE endpoints have been completely removed in favor of WebSocket-only implementation
}