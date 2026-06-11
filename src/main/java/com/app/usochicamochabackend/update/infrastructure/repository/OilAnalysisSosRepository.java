package com.app.usochicamochabackend.update.infrastructure.repository;

import com.app.usochicamochabackend.update.infrastructure.entity.OilAnalysisSosEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para análisis SOS (Scheduled Oil Sampling) en maquinaria
 */
@Repository
public interface OilAnalysisSosRepository extends JpaRepository<OilAnalysisSosEntity, Long> {

    /**
     * Obtiene el último análisis SOS para una máquina
     */
    Optional<OilAnalysisSosEntity> findTopByMachineIdAndIsApprovedTrueOrderByAnalysisDateDesc(
        Long machineId
    );

    /**
     * Obtiene todos los análisis SOS para una máquina
     */
    List<OilAnalysisSosEntity> findByMachineIdOrderByAnalysisDateDesc(Long machineId);

    /**
     * Obtiene análisis SOS aprobados en un rango de fechas
     */
    List<OilAnalysisSosEntity> findByMachineIdAndIsApprovedTrueAndAnalysisDateBetweenOrderByAnalysisDateDesc(
        Long machineId,
        LocalDateTime from,
        LocalDateTime to
    );

    /**
     * Obtiene análisis SOS pendientes de aprobación
     */
    @Query("SELECT a FROM OilAnalysisSosEntity a WHERE a.machineId = :machineId AND a.isApproved = false ORDER BY a.analysisDate DESC")
    List<OilAnalysisSosEntity> findPendingApprovalByMachineId(@Param("machineId") Long machineId);

    /**
     * Cuenta análisis SOS aprobados para una máquina
     */
    long countByMachineIdAndIsApprovedTrue(Long machineId);
}
