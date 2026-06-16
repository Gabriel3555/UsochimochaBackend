package com.app.usochicamochabackend.notifications.infrastructure.repository;

import com.app.usochicamochabackend.notifications.infrastructure.entity.AlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<AlertEntity, Long> {

    /**
     * Busca alertas activas para una placa específica
     */
    List<AlertEntity> findByPlacaAndEstado(String placa, String estado);

    /**
     * Busca la alerta más reciente de un tipo específico para una placa
     */
    Optional<AlertEntity> findTopByPlacaAndTipoAlertaAndEstadoOrderByFechaCreacionDesc(
        String placa,
        String tipoAlerta,
        String estado
    );

    /**
     * Busca todas las alertas activas
     */
    List<AlertEntity> findByEstado(String estado);

    /**
     * Busca alertas por tipo
     */
    List<AlertEntity> findByTipoAlerta(String tipoAlerta);

    /**
     * Busca alertas próximas a vencer (fecha_vencimiento <= hoy + 30 días)
     */
    @Query("SELECT a FROM AlertEntity a WHERE a.estado = 'ACTIVE' " +
           "AND a.fechaVencimiento <= :threshold " +
           "AND a.colorEstado IN ('ROJO', 'AMARILLO')")
    List<AlertEntity> findAlertasProximasAVencer(@Param("threshold") LocalDate threshold);

    /**
     * Busca alertas vencidas (fecha_vencimiento < hoy)
     */
    @Query("SELECT a FROM AlertEntity a WHERE a.estado = 'ACTIVE' " +
           "AND a.fechaVencimiento < CURRENT_DATE " +
           "AND a.colorEstado = 'ROJO'")
    List<AlertEntity> findAlertasVencidas();

    /**
     * Busca alertas para una placa específica
     */
    List<AlertEntity> findByPlacaOrderByFechaCreacionDesc(String placa);

    /**
     * Busca alertas por placa y tipo
     */
    List<AlertEntity> findByPlacaAndTipoAlertaOrderByFechaCreacionDesc(String placa, String tipoAlerta);

    /**
     * Cuenta alertas activas por tipo
     */
    @Query("SELECT COUNT(a) FROM AlertEntity a WHERE a.estado = 'ACTIVE' AND a.tipoAlerta = :tipo")
    long countActiveAlertsByType(@Param("tipo") String tipo);

    /**
     * Cuenta alertas activas totales
     */
    @Query("SELECT COUNT(a) FROM AlertEntity a WHERE a.estado = 'ACTIVE'")
    long countActiveAlerts();

    /**
     * Busca una alerta por su UUID
     */
    Optional<AlertEntity> findByUuid(String uuid);
}
