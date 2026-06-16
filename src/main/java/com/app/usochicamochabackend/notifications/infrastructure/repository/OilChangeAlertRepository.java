package com.app.usochicamochabackend.notifications.infrastructure.repository;

import com.app.usochicamochabackend.notifications.infrastructure.entity.OilChangeAlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository para auditoría de alertas de cambio de aceite.
 */
@Repository
public interface OilChangeAlertRepository extends JpaRepository<OilChangeAlertEntity, Long> {

    /**
     * Busca una alerta por su UUID.
     */
    Optional<OilChangeAlertEntity> findByUuid(String uuid);

    /**
     * Obtiene todas las alertas para una placa específica.
     */
    List<OilChangeAlertEntity> findByPlacaOrderByCreatedAtDesc(String placa);

    /**
     * Obtiene alertas de un color específico (YELLOW, RED).
     */
    List<OilChangeAlertEntity> findByAlertColorAndStatusOrderByCreatedAtDesc(String alertColor, Boolean status);

    /**
     * Obtiene alertas dentro de un rango de tiempo.
     */
    @Query("SELECT o FROM OilChangeAlertEntity o WHERE o.createdAt >= :from AND o.createdAt <= :to AND o.status = true ORDER BY o.createdAt DESC")
    List<OilChangeAlertEntity> findAlertsInTimeRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Obtiene la última alerta para una placa.
     */
    @Query("SELECT o FROM OilChangeAlertEntity o WHERE o.placa = :placa AND o.status = true ORDER BY o.createdAt DESC LIMIT 1")
    OilChangeAlertEntity findLastAlertByPlaca(@Param("placa") String placa);

    /**
     * Cuenta alertas RED sin resolver (para dashboards).
     */
    long countByAlertColorAndStatus(String alertColor, Boolean status);
}
