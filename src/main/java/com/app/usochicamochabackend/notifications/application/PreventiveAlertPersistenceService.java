package com.app.usochicamochabackend.notifications.application;

import com.app.usochicamochabackend.notifications.application.dto.PreventiveAlertDTO;
import com.app.usochicamochabackend.notifications.infrastructure.entity.PreventiveAlertEntity;
import com.app.usochicamochabackend.notifications.infrastructure.repository.PreventiveAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Escribe alertas preventivas en su propia transacción (REQUIRES_NEW), separada de la
 * transacción de lectura de {@link PreventiveAlertCalculationService#calculateAndEmitAlerts()}.
 * Así, si un registro puntual falla al guardarse (ej. overflow numérico), esa transacción
 * se revierte sola y no arrastra al resto del lote calculado en el mismo ciclo.
 */
@Service
@RequiredArgsConstructor
public class PreventiveAlertPersistenceService {

    private final PreventiveAlertRepository alertRepository;
    private final NotificationService notificationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveOrUpdateAlert(
        String assetId,
        String assetType,
        String alertType,
        String alertSubtype,
        String colorEstado,
        String descripcion,
        LocalDate fechaVencimiento,
        String metricType,
        Double metricValue,
        Double percentageUsed
    ) {
        var existing = alertRepository.findActiveAlertForAsset(assetId, alertType, alertSubtype);

        PreventiveAlertEntity alert;
        if (existing.isPresent()) {
            alert = existing.get();
            alert.setColorEstado(colorEstado);
            alert.setDescripcion(descripcion);
            alert.setFechaVencimiento(fechaVencimiento);
            alert.setMetricValue(metricValue);
            alert.setPercentageUsed(percentageUsed);
            alert.setFechaActualizacion(LocalDateTime.now());
        } else {
            alert = PreventiveAlertEntity.builder()
                .assetId(assetId)
                .assetType(assetType)
                .alertType(alertType)
                .alertSubtype(alertSubtype)
                .colorEstado(colorEstado)
                .estado("ACTIVA")
                .descripcion(descripcion)
                .fechaVencimiento(fechaVencimiento)
                .metricType(metricType)
                .metricValue(metricValue)
                .percentageUsed(percentageUsed)
                .fechaCreacion(LocalDateTime.now())
                .status(true)
                .build();
        }

        alertRepository.save(alert);
        notificationService.notifyPreventiveAlert(PreventiveAlertDTO.fromEntity(alert));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteActiveAlertForAsset(String assetId, String alertType) {
        alertRepository.deleteActiveAlertForAsset(assetId, alertType);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteActiveAlertForAssetAndSubtype(String assetId, String alertType, String alertSubtype) {
        alertRepository.deleteActiveAlertForAssetAndSubtype(assetId, alertType, alertSubtype);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanupGreenAlerts() {
        alertRepository.deleteGreenAlerts();
    }
}
