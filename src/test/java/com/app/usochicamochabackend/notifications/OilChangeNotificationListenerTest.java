package com.app.usochicamochabackend.notifications;

import com.app.usochicamochabackend.machine.application.dto.MachineMonitoringDTO;
import com.app.usochicamochabackend.machine.application.service.MachineMonitoringService;
import com.app.usochicamochabackend.notifications.application.service.NotificationWebSocketService;
import com.app.usochicamochabackend.notifications.application.service.OilChangeNotificationListener;
import com.app.usochicamochabackend.shared.event.InspectionCompletedEvent;
import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test para verificar que OilChangeNotificationListener procesa máquinas (MAQUINARIA) correctamente.
 */
class OilChangeNotificationListenerTest {

    @Mock
    private MachineMonitoringService machineMonitoringService;

    @Mock
    private NotificationWebSocketService notificationWebSocketService;

    @Mock
    private SaveActionUseCase saveActionUseCase;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private OilChangeNotificationListener listener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new OilChangeNotificationListener(
            null, // vehicleMonitoringService (no usado en este test)
            null, // motoMonitoringService (no usado en este test)
            machineMonitoringService,
            notificationWebSocketService,
            saveActionUseCase,
            messagingTemplate
        );
    }

    @Test
    void testMachineAlertProcessedWhenYellowAlertDetected() {
        // ARRANGE: Crear evento de inspección para máquina
        Long machineId = 1L;
        InspectionCompletedEvent event = new InspectionCompletedEvent(
            this,
            machineId,
            "Máquina #1",
            "MAQUINARIA",
            8500L,  // 8500 horas
            LocalDateTime.now()
        );

        // Crear DTO de máquina con alerta YELLOW
        MachineMonitoringDTO.OilChangeInfoDTO motorOil = new MachineMonitoringDTO.OilChangeInfoDTO(
            "MOTOR_OIL",
            "Shell Rimula",
            50.0,
            8000,
            10000,
            85.0,  // 85% uso → YELLOW
            "YELLOW",
            "🟡 Próximo a cambio de aceite de motor"
        );

        MachineMonitoringDTO machine = new MachineMonitoringDTO(
            machineId,
            "Excavadora CAT",
            8500,
            motorOil,
            null,  // sin aceite hidráulico
            null
        );

        // Mock: machineMonitoringService devuelve la máquina con alerta
        when(machineMonitoringService.getConsolidateById(machineId)).thenReturn(machine);

        // ACT: Procesar evento
        listener.onInspectionCompleted(event);

        // ASSERT: Verificar que se envió notificación
        verify(notificationWebSocketService, times(1))
            .broadcastOilChangeAlert((MachineMonitoringDTO) machine);

        verify(messagingTemplate, times(1))
            .convertAndSend(
                "/topic/notifications/data-update",
                "oil-change-alert-updated"
            );

        verify(saveActionUseCase, times(1)).save(any(String.class));
    }

    @Test
    void testMachineAlertProcessedWhenRedAlertDetected() {
        // ARRANGE: Evento con alerta RED (cambio urgente)
        Long machineId = 2L;
        InspectionCompletedEvent event = new InspectionCompletedEvent(
            this,
            machineId,
            "Máquina #2",
            "MAQUINARIA",
            10500L,  // 10500 horas
            LocalDateTime.now()
        );

        // Máquina con alerta RED en hidráulico
        MachineMonitoringDTO.OilChangeInfoDTO hydraulicOil = new MachineMonitoringDTO.OilChangeInfoDTO(
            "HYDRAULIC_OIL",
            "Mobil DTE",
            100.0,
            10000,
            10000,
            105.0,  // 105% uso → RED (URGENTE)
            "RED",
            "🔴 URGENTE: Cambio de aceite hidráulico vencido"
        );

        MachineMonitoringDTO machine = new MachineMonitoringDTO(
            machineId,
            "Motoniveladora CAT",
            10500,
            null,
            hydraulicOil,
            null
        );

        when(machineMonitoringService.getConsolidateById(machineId)).thenReturn(machine);

        // ACT
        listener.onInspectionCompleted(event);

        // ASSERT: Se debe notificar
        verify(notificationWebSocketService, times(1))
            .broadcastOilChangeAlert(machine);

        verify(messagingTemplate, times(1))
            .convertAndSend(
                eq("/topic/notifications/data-update"),
                eq("oil-change-alert-updated")
            );
    }

    @Test
    void testMachineAlertNotProcessedWhenGreenAlert() {
        // ARRANGE: Máquina sin alertas (TODO OK)
        Long machineId = 3L;
        InspectionCompletedEvent event = new InspectionCompletedEvent(
            this,
            machineId,
            "Máquina #3",
            "MAQUINARIA",
            3000L,
            LocalDateTime.now()
        );

        MachineMonitoringDTO.OilChangeInfoDTO motorOil = new MachineMonitoringDTO.OilChangeInfoDTO(
            "MOTOR_OIL",
            "Castrol",
            40.0,
            0,
            10000,
            30.0,  // 30% uso → GREEN (OK)
            "GREEN",
            "✅ Sistema de aceite en buen estado"
        );

        MachineMonitoringDTO machine = new MachineMonitoringDTO(
            machineId,
            "Bomba Diesel",
            3000,
            motorOil,
            null,
            null
        );

        when(machineMonitoringService.getConsolidateById(machineId)).thenReturn(machine);

        // ACT
        listener.onInspectionCompleted(event);

        // ASSERT: No debe notificar (GREEN no es alerta)
        verify(notificationWebSocketService, never())
            .broadcastOilChangeAlert((MachineMonitoringDTO) any());

        verify(messagingTemplate, never())
            .convertAndSend(anyString(), anyString());
    }

    @Test
    void testMachineAlertTakesMostCriticalWhenBothOils() {
        // ARRANGE: Máquina con alertas en AMBOS aceites (toma la más crítica)
        Long machineId = 4L;
        InspectionCompletedEvent event = new InspectionCompletedEvent(
            this,
            machineId,
            "Máquina #4",
            "MAQUINARIA",
            9500L,
            LocalDateTime.now()
        );

        MachineMonitoringDTO.OilChangeInfoDTO motorOil = new MachineMonitoringDTO.OilChangeInfoDTO(
            "MOTOR_OIL",
            "Shell",
            50.0,
            8000,
            10000,
            85.0,  // YELLOW
            "YELLOW",
            "🟡 Motor próximo a cambio"
        );

        MachineMonitoringDTO.OilChangeInfoDTO hydraulicOil = new MachineMonitoringDTO.OilChangeInfoDTO(
            "HYDRAULIC_OIL",
            "Mobil",
            100.0,
            10000,
            10000,
            95.0,  // Más crítico que YELLOW
            "RED",
            "🔴 Hidráulico URGENTE"
        );

        MachineMonitoringDTO machine = new MachineMonitoringDTO(
            machineId,
            "Excavadora",
            9500,
            motorOil,
            hydraulicOil,
            null
        );

        when(machineMonitoringService.getConsolidateById(machineId)).thenReturn(machine);

        // ACT
        listener.onInspectionCompleted(event);

        // ASSERT: Debe notificar (hay RED)
        verify(notificationWebSocketService, times(1))
            .broadcastOilChangeAlert((MachineMonitoringDTO) machine);
    }
}
