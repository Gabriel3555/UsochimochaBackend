package com.app.usochicamochabackend.update.application.dto;

import com.app.usochicamochabackend.update.infrastructure.entity.OilAnalysisSosEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * DTO para análisis SOS (Scheduled Oil Sampling)
 * Permite crear y consultar análisis de laboratorio de aceite para maquinaria
 */
@Builder
public record OilAnalysisSosDTO(
    @Schema(description = "ID del análisis", example = "1")
    Long id,

    @Schema(description = "ID de la máquina", example = "10")
    Long machineId,

    @Schema(description = "Nombre de la máquina", example = "Excavadora CAT 320")
    String machineName,

    @Schema(description = "Fecha del análisis", example = "2026-06-10T14:30:00")
    LocalDateTime analysisDate,

    @Schema(description = "Tipo de aceite analizado", example = "SYNTHETIC")
    String oilType,

    @Schema(description = "Horas de motor cuando se tomó la muestra", example = "750")
    Integer hoursAtAnalysis,

    @Schema(description = "Próximo cambio recomendado (horas)", example = "850")
    Integer nextChangeHours,

    @Schema(description = "URL del reporte del laboratorio", example = "https://laboratorio.com/report-123.pdf")
    String sosReportUrl,

    @Schema(description = "Mecánico que autorizó el análisis", example = "Juan Pérez")
    String approvedByMechanic,

    @Schema(description = "Observaciones adicionales", example = "Aceite en excelente estado, sin contaminación")
    String observations,

    @Schema(description = "¿Fue aprobado por supervisor?", example = "true")
    Boolean isApproved,

    @Schema(description = "Extensión de horas autorizada", example = "50")
    Integer extendedHours,

    @Schema(description = "¿Autoriza extensión?", example = "true")
    Boolean authorizesExtension,

    @Schema(description = "Fecha de creación", example = "2026-06-10T14:30:00")
    LocalDateTime createdAt
) {
    public static OilAnalysisSosDTO fromEntity(OilAnalysisSosEntity entity) {
        Integer standardRequirement = 750; // TODO: Obtener del requisito configurado
        return OilAnalysisSosDTO.builder()
            .id(entity.getId())
            .machineId(entity.getMachineId())
            .machineName(entity.getMachineName())
            .analysisDate(entity.getAnalysisDate())
            .oilType(entity.getOilType() != null ? entity.getOilType().name() : null)
            .hoursAtAnalysis(entity.getHoursAtAnalysis())
            .nextChangeHours(entity.getNextChangeHours())
            .sosReportUrl(entity.getSosReportUrl())
            .approvedByMechanic(entity.getApprovedByMechanic())
            .observations(entity.getObservations())
            .isApproved(entity.getIsApproved())
            .extendedHours(entity.getExtendedHours(standardRequirement))
            .authorizesExtension(entity.authorizesExtension(standardRequirement))
            .createdAt(entity.getCreatedAt())
            .build();
    }
}
