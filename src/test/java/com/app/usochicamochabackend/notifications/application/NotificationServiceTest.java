package com.app.usochicamochabackend.notifications.application;

import com.app.usochicamochabackend.notifications.infrastructure.websocket.NotificationWebSocketHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationWebSocketHandler webSocketHandler;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("notifyInspection: reenvía al handler cuando hay datos")
    void notifyInspection_ConDatos_LlamaHandler() {
        notificationService.notifyInspection("{\"tipo\":\"INSPECCION\"}");
        verify(webSocketHandler).broadcastInspection(anyString());
    }

    @Test
    @DisplayName("notifyInspection: ignora mensaje null")
    void notifyInspection_Null_NoLlamaHandler() {
        notificationService.notifyInspection(null);
        verifyNoInteractions(webSocketHandler);
    }

    @Test
    @DisplayName("notifyOilChange: reenvía al handler cuando hay datos")
    void notifyOilChange_ConDatos_LlamaHandler() {
        notificationService.notifyOilChange("{\"tipo\":\"ACEITE\"}");
        verify(webSocketHandler).broadcastOilChange(anyString());
    }

    @Test
    @DisplayName("notifyOilChange: ignora mensaje null")
    void notifyOilChange_Null_NoLlamaHandler() {
        notificationService.notifyOilChange(null);
        verifyNoInteractions(webSocketHandler);
    }

    @Test
    @DisplayName("notifyUser: envía al usuario correcto cuando ambos no son null")
    void notifyUser_ConDatos_LlamaHandler() {
        notificationService.notifyUser("admin", "Alerta de aceite");
        verify(webSocketHandler).sendToUser("admin", "Alerta de aceite");
    }

    @Test
    @DisplayName("notifyUser: ignora si usuario o mensaje son null")
    void notifyUser_Null_NoLlamaHandler() {
        notificationService.notifyUser(null, "Alerta");
        notificationService.notifyUser("admin", null);
        verifyNoInteractions(webSocketHandler);
    }
}
