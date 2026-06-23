package com.app.usochicamochabackend.notifications.infrastructure.repository;

import com.app.usochicamochabackend.notifications.infrastructure.entity.PreventiveAlertEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreventiveAlertRepository extends JpaRepository<PreventiveAlertEntity, Long> {

    /**
     * Obtiene una alerta ACTIVA para un activo específico.
     * Busca por assetId, alertType, y opcionalmente alertSubtype.
     */
    @Query("SELECT p FROM PreventiveAlertEntity p " +
           "WHERE p.assetId = :assetId " +
           "AND p.alertType = :alertType " +
           "AND p.estado = 'ACTIVA' " +
           "AND p.status = true " +
           "AND (p.alertSubtype = :alertSubtype OR (:alertSubtype IS NULL AND p.alertSubtype IS NULL))")
    Optional<PreventiveAlertEntity> findActiveAlertForAsset(
        @Param("assetId") String assetId,
        @Param("alertType") String alertType,
        @Param("alertSubtype") String alertSubtype
    );

    /**
     * Elimina alertas ACTIVAS de un tipo específico para un assetId.
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM PreventiveAlertEntity p " +
           "WHERE p.assetId = :assetId " +
           "AND p.alertType = :alertType " +
           "AND p.estado = 'ACTIVA'")
    void deleteActiveAlertForAsset(
        @Param("assetId") String assetId,
        @Param("alertType") String alertType
    );

    /**
     * Elimina todas las alertas VERDES (no se deben guardar).
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM PreventiveAlertEntity p WHERE p.colorEstado = 'VERDE'")
    void deleteGreenAlerts();

    /**
     * Obtiene todas las alertas en un estado específico.
     */
    List<PreventiveAlertEntity> findByEstado(String estado);

    /**
     * Obtiene todas las alertas activas de un assetId.
     */
    List<PreventiveAlertEntity> findByAssetIdAndEstado(String assetId, String estado);

    /**
     * Obtiene todas las alertas activas de un tipo específico.
     */
    List<PreventiveAlertEntity> findByAlertTypeAndEstado(String alertType, String estado);

    /**
     * Obtiene alertas por tipo de activo y estado.
     */
    List<PreventiveAlertEntity> findByAssetTypeAndEstado(String assetType, String estado);

    /**
     * Obtiene alertas con paginación por status (para REST API).
     */
    Page<PreventiveAlertEntity> findByStatus(Boolean status, Pageable pageable);

    /**
     * Obtiene alertas con paginación por estado y status.
     */
    Page<PreventiveAlertEntity> findByEstadoAndStatus(String estado, Boolean status, Pageable pageable);

    /**
     * Obtiene alertas de un activo específico con paginación.
     */
    Page<PreventiveAlertEntity> findByAssetIdAndStatus(String assetId, Boolean status, Pageable pageable);

    /**
     * Cuenta alertas por color y status.
     */
    long countByColorEstadoAndStatus(String colorEstado, Boolean status);

    /**
     * Obtiene alertas por tipo con paginación.
     */
    Page<PreventiveAlertEntity> findByAlertType(String alertType, Pageable pageable);
}
