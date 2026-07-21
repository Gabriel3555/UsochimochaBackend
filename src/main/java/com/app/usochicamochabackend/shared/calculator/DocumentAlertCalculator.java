package com.app.usochicamochabackend.shared.calculator;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

public class DocumentAlertCalculator {

    public static class Result {
        public String colorEstado;
        public Long diasRestantes;
        public String descripcion;

        public Result(String colorEstado, Long diasRestantes, String descripcion) {
            this.colorEstado = colorEstado;
            this.diasRestantes = diasRestantes;
            this.descripcion = descripcion;
        }
    }

    /**
     * Calcula alerta para documento con fecha de vencimiento.
     * Retorna: { colorEstado, diasRestantes, descripcion }
     *
     * Lógica estándar (SOAT, TECNOMECANICA, RUNT, LICENCIA):
     * - Si fecha_vencimiento < HOY → ROJO (vencido)
     * - Si fecha_vencimiento <= HOY + 7 días → ROJO (urgente)
     * - Si fecha_vencimiento <= HOY + 45 días → AMARILLO (advertencia)
     * - Si fecha_vencimiento > HOY + 45 días → VERDE (no se guarda)
     *
     * Caso especial EXTINTOR (cuenta por MESES):
     * - Se calcula por MESES (cuenta de 11 en 11)
     * - Mismas franjas pero en meses
     */
    public static Result calculateDocumentAlert(
        String documentType,        // "SOAT", "TECNOMECANICA", "EXTINTOR", "RUNT", "LICENCIA"
        LocalDate expirationDate
    ) {
        if (expirationDate == null) {
            return new Result("VERDE", 0L, "Sin fecha de vencimiento");
        }

        LocalDate today = LocalDate.now();
        long diasRestantes = ChronoUnit.DAYS.between(today, expirationDate);

        if ("EXTINTOR".equals(documentType)) {
            return calculateExtintorAlert(today, expirationDate, diasRestantes);
        } else {
            return calculateStandardDocumentAlert(documentType, today, expirationDate, diasRestantes);
        }
    }

    private static Result calculateStandardDocumentAlert(
        String documentType,
        LocalDate today,
        LocalDate expirationDate,
        long diasRestantes
    ) {
        if (diasRestantes < 0) {
            return new Result(
                "ROJO",
                diasRestantes,
                documentType + " VENCIDO hace " + Math.abs(diasRestantes) + " días"
            );
        } else if (diasRestantes <= 7) {
            return new Result(
                "ROJO",
                diasRestantes,
                documentType + " - VENCIMIENTO URGENTE (" + diasRestantes + " días)"
            );
        } else if (diasRestantes <= 45) {
            return new Result(
                "AMARILLO",
                diasRestantes,
                documentType + " próximo a vencer (" + diasRestantes + " días)"
            );
        } else {
            return new Result(
                "VERDE",
                diasRestantes,
                documentType + " en buen estado"
            );
        }
    }

    private static Result calculateExtintorAlert(
        LocalDate today,
        LocalDate expirationDate,
        long diasRestantes
    ) {
        YearMonth mesActual = YearMonth.from(today);
        YearMonth mesVencimiento = YearMonth.from(expirationDate);
        YearMonth mesMenosUnMes = mesVencimiento.minusMonths(1);

        if (mesActual.isAfter(mesVencimiento)) {
            // Ya pasó el mes de vencimiento
            return new Result(
                "ROJO",
                diasRestantes,
                "EXTINTOR VENCIDO"
            );
        } else if (mesActual.equals(mesVencimiento)) {
            // Estamos en el mes de vencimiento
            return new Result(
                "ROJO",
                diasRestantes,
                "EXTINTOR - VENCIMIENTO EN CURSO (" + diasRestantes + " días)"
            );
        } else if (mesActual.equals(mesMenosUnMes)) {
            // Un mes antes - mostrar meses y días
            long meses = diasRestantes / 30;
            long dias = diasRestantes % 30;
            String timeStr = meses > 0 ? meses + " mes" + (meses > 1 ? "es" : "") : "";
            if (dias > 0) {
                timeStr += (meses > 0 ? " y " : "") + dias + " día" + (dias > 1 ? "s" : "");
            }
            return new Result(
                "AMARILLO",
                diasRestantes,
                "EXTINTOR próximo a vencer (" + timeStr + ")"
            );
        } else {
            // Más de mes y medio
            return new Result(
                "VERDE",
                diasRestantes,
                "EXTINTOR en buen estado"
            );
        }
    }
}
