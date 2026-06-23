package com.app.usochicamochabackend.shared.calculator;

public class OilChangeVehicleAlertCalculator {

    public static class Result {
        public String colorEstado;
        public Double percentageUsed;
        public Integer kmRemaining;
        public String descripcion;

        public Result(String colorEstado, Double percentageUsed, Integer kmRemaining, String descripcion) {
            this.colorEstado = colorEstado;
            this.percentageUsed = percentageUsed;
            this.kmRemaining = kmRemaining;
            this.descripcion = descripcion;
        }
    }

    /**
     * Calcula alerta para CAMBIO DE ACEITE (vehículos/motos por KILOMETRAJE).
     * Retorna: { colorEstado, percentageUsed, kmRemaining, descripcion }
     *
     * Lógica de colores:
     * - Si percentageUsed >= 100 → ROJO (ya pasó el cambio)
     * - Si percentageUsed >= 90 → ROJO (crítico)
     * - Si percentageUsed >= 70 → AMARILLO (advertencia)
     * - Si percentageUsed < 70 → VERDE (no se guarda)
     *
     * Datos de entrada:
     * - kmActual: kilometraje actual del vehículo (VehicleEntity.kilometrajeActual)
     * - kmAtChange: km cuando se hizo el último cambio (VehicleOilChangeEntity.kmAtChange)
     * - intervalKm: intervalo entre cambios (VehicleOilChangeEntity.intervalKm)
     */
    public static Result calculateOilChangeAlert(
        Integer kmActual,
        Integer kmAtChange,
        Integer intervalKm
    ) {
        // Validar datos
        if (kmActual == null || kmAtChange == null || intervalKm == null || intervalKm <= 0) {
            return new Result("VERDE", 0.0, 0, "Datos incompletos para cálculo de alerta");
        }

        // Calcular métricas
        Integer kmSinceChange = kmActual - kmAtChange;
        Integer nextChangeKm = kmAtChange + intervalKm;
        Integer kmRemaining = nextChangeKm - kmActual;
        Double percentageUsed = (double) (kmSinceChange * 100) / intervalKm;

        // Determinar color y descripción
        if (percentageUsed >= 100) {
            return new Result(
                "ROJO",
                percentageUsed,
                kmRemaining,
                "🚗 CAMBIO DE ACEITE VENCIDO (" + Math.abs(kmRemaining) + " km atrás)"
            );
        } else if (percentageUsed >= 90) {
            return new Result(
                "ROJO",
                percentageUsed,
                kmRemaining,
                "🚗 CAMBIO DE ACEITE CRÍTICO (" + kmRemaining + " km restantes)"
            );
        } else if (percentageUsed >= 70) {
            return new Result(
                "AMARILLO",
                percentageUsed,
                kmRemaining,
                "🚗 PRÓXIMO CAMBIO DE ACEITE (" + kmRemaining + " km restantes)"
            );
        } else {
            return new Result(
                "VERDE",
                percentageUsed,
                kmRemaining,
                "Aceite en buen estado"
            );
        }
    }
}
