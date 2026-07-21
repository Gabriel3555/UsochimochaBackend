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
import java.util.Optional;

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
        var existing = alertRepository.findActiveAlertForAsset(assetId, alertType, null);
        alertRepository.deleteActiveAlertForAsset(assetId, alertType);
        notifyResolvedIfPresent(existing);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteActiveAlertForAssetAndSubtype(String assetId, String alertType, String alertSubtype) {
        var existing = alertRepository.findActiveAlertForAsset(assetId, alertType, alertSubtype);
        alertRepository.deleteActiveAlertForAssetAndSubtype(assetId, alertType, alertSubtype);
        notifyResolvedIfPresent(existing);
    }

    /**
     * Avisa por WebSocket que una alerta se resolvió, para que el frontend la quite del store
     * en vivo (sin esto, la alerta se borra en BD pero el navegador nunca se entera y queda
     * visible hasta que el usuario recarga la página).
     *
     * OJO: no mutar la entidad gestionada por JPA aquí. El borrado de arriba es un bulk
     * @Modifying DELETE que no sincroniza el contexto de persistencia; si se llama
     * alert.setEstado(...) sobre la entidad ya cargada, Hibernate la marca "sucia" y, al
     * hacer flush al terminar la transacción, intenta reescribirla — chocando con el DELETE
     * que ya se ejecutó en la misma transacción. Por eso se arma el DTO aparte con withEstado().
     */
    private void notifyResolvedIfPresent(Optional<PreventiveAlertEntity> existing) {
        existing.ifPresent(alert ->
            notificationService.notifyPreventiveAlert(PreventiveAlertDTO.fromEntity(alert).withEstado("RESUELTA")));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanupGreenAlerts() {
        alertRepository.deleteGreenAlerts();
    }
}
