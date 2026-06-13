package com.app.usochicamochabackend.shared.calculator;

import com.app.usochicamochabackend.shared.dto.AlertStatus;

/**
 * Calculadora unificada de alertas de cambio de aceite.
 *
 * Aplica a: vehículos (km), motos (km), máquinas (horas).
 * Lógica: % de uso del intervalo configurado.
 */
public class OilChangeAlertCalculator {

    // Umbrales de % de uso
    private static final double THRESHOLD_PROGRAMMED = 60.0;  // 60%: Programado (BLUE)
    private static final double THRESHOLD_YELLOW = 80.0;      // 80%: Próximo a cambio (YELLOW)
    private static final double THRESHOLD_CRITICAL = 100.0;   // 100%: Cambio urgente (RED)

    /**
     * Calcula el estado de alerta basado en % de uso del intervalo.
     *
     * @param distanceSinceLastChange km recorridos O horas trabajadas desde último cambio
     * @param intervalDistance intervalo configurado en km O horas
     * @return AlertStatus con estado, color, % uso y mensaje
     * @throws IllegalArgumentException si los parámetros son inválidos
     */
    public static AlertStatus calculateAlert(long distanceSinceLastChange, long intervalDistance) {
        // Validaciones
        if (intervalDistance <= 0) {
            return AlertStatus.unknown("Intervalo no configurado", -1);
        }

        if (distanceSinceLastChange < 0) {
            return AlertStatus.error("Error: distancia negativa", -1);
        }

        // Cálculo del % de uso
        double percentageUsed = (distanceSinceLastChange * 100.0) / intervalDistance;
        long distanceRemaining = intervalDistance - distanceSinceLastChange;

        // Mapeo a estado y color según thresholds
        if (percentageUsed >= THRESHOLD_CRITICAL) {
            return new AlertStatus(
                "Cambio de Aceite",
                "RED",
                "🔴 URGENTE: Cambio de aceite vencido. Riego en funcionamiento con sistema comprometido.",
                percentageUsed,
                distanceRemaining,
                null
            );
        } else if (percentageUsed >= THRESHOLD_YELLOW) {
            return new AlertStatus(
                "Próximo a Cambio",
                "YELLOW",
                "🟡 Próximo a cambio de aceite. Programar intervención dentro de " +
                    Math.max(0, distanceRemaining) + " km/horas.",
                percentageUsed,
                distanceRemaining,
                null
            );
        } else if (percentageUsed >= THRESHOLD_PROGRAMMED) {
            return new AlertStatus(
                "Programado",
                "BLUE",
                "🔵 Cambio de aceite programado. Quedan " + distanceRemaining + " km/horas.",
                percentageUsed,
                distanceRemaining,
                null
            );
        } else {
            return new AlertStatus(
                "OK",
                "GREEN",
                "✅ Sistema de aceite en buen estado. Quedan " + distanceRemaining + " km/horas.",
                percentageUsed,
                distanceRemaining,
                null
            );
        }
    }

    /**
     * Determina si se debe enviar notificación según el nivel.
     *
     * Requisito de negocio:
     * - YELLOW: notificar cuando quedan ~1000 km (en intervalo 3000 km = 67%)
     * - RED: notificar cuando quedan ~100 km (en intervalo 3000 km = 97%)
     *
     * Mapeado a: 80% de uso para YELLOW, 80%+ para RED.
     */
    public static boolean shouldNotify(long distanceSinceLastChange, long intervalDistance) {
        if (intervalDistance <= 0) return false;

        double percentageUsed = (distanceSinceLastChange * 100.0) / intervalDistance;

        // Notificar si está en zona YELLOW o RED
        return percentageUsed >= THRESHOLD_YELLOW;
    }

    /**
     * Obtiene el color como string para compatibilidad con DTOs.
     */
    public static String getAlertColor(double percentageUsed) {
        if (percentageUsed >= THRESHOLD_CRITICAL) {
            return "RED";
        } else if (percentageUsed >= THRESHOLD_YELLOW) {
            return "YELLOW";
        } else if (percentageUsed >= THRESHOLD_PROGRAMMED) {
            return "BLUE";
        } else {
            return "GREEN";
        }
    }

    /**
     * Calcula distancia restante hasta próximo cambio.
     */
    public static long calculateDistanceRemaining(long distanceSinceLastChange, long intervalDistance) {
        return Math.max(0, intervalDistance - distanceSinceLastChange);
    }

    /**
     * Calcula % de uso.
     */
    public static double calculatePercentageUsed(long distanceSinceLastChange, long intervalDistance) {
        if (intervalDistance <= 0) return -1;
        return Math.min(999.99, (distanceSinceLastChange * 100.0) / intervalDistance);
    }
}
