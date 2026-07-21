package com.app.usochicamochabackend.update.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * DTO para crear análisis SOS (Scheduled Oil Sampling) en maquinaria
 */
public record CreateOilAnalysisSosDTO(
    @NotNull(message = "ID de máquina es requerido")
    @Schema(description = "ID de la máquina", example = "10")
    Long machineId,

    @NotNull(message = "Nombre de máquina es requerido")
    @Schema(description = "Nombre de la máquina", example = "Excavadora CAT 320")
    String machineName,

    @NotNull(message = "Tipo de aceite es requerido")
    @Schema(description = "Tipo de aceite analizado (MINERAL, SEMI_SYNTHETIC, SYNTHETIC)", example = "SYNTHETIC")
    String oilType,

    @NotNull(message = "Horas en análisis es requerido")
    @PositiveOrZero(message = "Horas no pueden ser negativas")
    @Schema(description = "Horas de motor cuando se tomó la muestra", example = "750")
    Integer hoursAtAnalysis,

    @NotNull(message = "Próximo cambio recomendado es requerido")
    @PositiveOrZero(message = "Horas de cambio no pueden ser negativas")
    @Schema(description = "Próximo cambio recomendado (horas) según laboratorio", example = "900")
    Integer nextChangeHours,

    @NotNull(message = "URL del reporte es requerida")
    @Schema(description = "URL del reporte del laboratorio", example = "https://laboratorio.com/report-123.pdf")
    String sosReportUrl,

    @NotNull(message = "Mecánico aprobador es requerido")
    @Schema(description = "Nombre del mecánico que aprueba el análisis", example = "Juan Pérez")
    String approvedByMechanic,

    @Schema(description = "Observaciones adicionales", example = "Aceite en excelente estado, bajo nivel de contaminación")
    String observations
) {
}
