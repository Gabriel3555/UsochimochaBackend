package com.app.usochicamochabackend.notifications.application;

import com.app.usochicamochabackend.notifications.application.dto.PreventiveAlertDTO;
import com.app.usochicamochabackend.notifications.infrastructure.entity.PreventiveAlertEntity;
import com.app.usochicamochabackend.notifications.infrastructure.repository.PreventiveAlertRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Cuando una alerta se resuelve (documento renovado, cambio de aceite registrado, km editado,
 * etc.), el borrado debe avisar por WebSocket con estado RESUELTA — de lo contrario, la fila
 * desaparece en BD pero el frontend nunca se entera y la alerta queda visible hasta que el
 * usuario recarga la página manualmente.
 */
@ExtendWith(MockitoExtension.class)
class PreventiveAlertPersistenceServiceTest {

    @Mock
    private PreventiveAlertRepository alertRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PreventiveAlertPersistenceService service;

    private PreventiveAlertEntity buildActiveAlert() {
        return PreventiveAlertEntity.builder()
                .id(1L)
                .assetId("ABC123")
                .assetType("VEHICULO")
                .alertType("OIL_CHANGE_VEHICLE")
                .alertSubtype(null)
                .colorEstado("ROJO")
                .estado("ACTIVA")
                .fechaCreacion(LocalDateTime.now())
                .status(true)
                .build();
    }

    @Test
    @DisplayName("deleteActiveAlertForAsset: notifica RESUELTA si había una alerta activa")
    void deleteActiveAlertForAsset_NotificaResuelta() {
        PreventiveAlertEntity existing = buildActiveAlert();
        when(alertRepository.findActiveAlertForAsset("ABC123", "OIL_CHANGE_VEHICLE", null))
                .thenReturn(Optional.of(existing));

        service.deleteActiveAlertForAsset("ABC123", "OIL_CHANGE_VEHICLE");

        verify(alertRepository).deleteActiveAlertForAsset("ABC123", "OIL_CHANGE_VEHICLE");

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(notificationService).notifyPreventiveAlert(captor.capture());
        PreventiveAlertDTO dto = (PreventiveAlertDTO) captor.getValue();
        assertEquals("RESUELTA", dto.estado());
        assertEquals("ABC123", dto.assetId());
    }

    @Test
    @DisplayName("deleteActiveAlertForAsset: no notifica nada si no había alerta activa")
    void deleteActiveAlertForAsset_SinAlertaPrevia_NoNotifica() {
        when(alertRepository.findActiveAlertForAsset("ABC123", "OIL_CHANGE_VEHICLE", null))
                .thenReturn(Optional.empty());

        service.deleteActiveAlertForAsset("ABC123", "OIL_CHANGE_VEHICLE");

        verify(alertRepository).deleteActiveAlertForAsset("ABC123", "OIL_CHANGE_VEHICLE");
        verify(notificationService, never()).notifyPreventiveAlert(any());
    }

    @Test
    @DisplayName("deleteActiveAlertForAssetAndSubtype: notifica RESUELTA respetando el subtipo")
    void deleteActiveAlertForAssetAndSubtype_NotificaResuelta() {
        PreventiveAlertEntity existing = buildActiveAlert();
        existing.setAlertType("DOCUMENTO");
        existing.setAlertSubtype("SOAT");
        when(alertRepository.findActiveAlertForAsset("ABC123", "DOCUMENTO", "SOAT"))
                .thenReturn(Optional.of(existing));

        service.deleteActiveAlertForAssetAndSubtype("ABC123", "DOCUMENTO", "SOAT");

        verify(alertRepository).deleteActiveAlertForAssetAndSubtype("ABC123", "DOCUMENTO", "SOAT");

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(notificationService).notifyPreventiveAlert(captor.capture());
        PreventiveAlertDTO dto = (PreventiveAlertDTO) captor.getValue();
        assertEquals("RESUELTA", dto.estado());
        assertEquals("SOAT", dto.alertSubtype());
    }
}
