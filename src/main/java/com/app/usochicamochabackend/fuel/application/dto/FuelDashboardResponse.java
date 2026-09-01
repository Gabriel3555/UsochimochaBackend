package com.app.usochicamochabackend.fuel.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FuelDashboardResponse(
    LocalDate fechaInicio,
    LocalDate fechaFin,
    BigDecimal totalComprasAlmacen,
    BigDecimal totalTanqueosBomba,
    BigDecimal totalDescuentos,
    BigDecimal descuentoMensual,
    BigDecimal gastoBruto,
    BigDecimal gastoNeto,
    BigDecimal ahorro,
    List<GalonesPorTipo> galonesPorTipo,
    List<GastoPorTipo> gastoPorTipo,
    // Cantidad SOLO de bomba por tipo (subconjunto de galonesPorTipo, que mezcla
    // bomba+almacén) — sirve para que el frontend calcule un precio/unidad real
    // (gastoPorTipo ÷ galonesBombaPorTipo) sin diluirlo con galones de almacén,
    // que no tienen costo asociado.
    List<GalonesPorTipo> galonesBombaPorTipo,
    // Discrepancias FINANCIERAS (ingresado ≠ calculado) de compras/tanqueos — no
    // incluye alertas de rendimiento, ver alertasRendimiento más abajo (son dos
    // universos de anomalía distintos, cada uno con su propia tarjeta en el front).
    Long discrepancias,
    // Alertas de RENDIMIENTO (consumo real fuera de lo esperado, ver
    // FuelPerformanceService) en el mismo rango — MAQUINARIA + VEHICULO + MOTOCICLETA.
    Long alertasRendimiento,
    BigDecimal precioPromedioGalonComprado,
    ComparacionAnterior comparacionAnterior
) {
    public record GalonesPorTipo(Long fuelTypeId, BigDecimal cantidad) {}

    /** Gasto total (compras almacén + tanqueos bomba) por tipo de combustible, en pesos. */
    public record GastoPorTipo(Long fuelTypeId, BigDecimal monto) {}

    /**
     * Comparación contra el periodo inmediatamente anterior, de la misma duración
     * (no "mes anterior" fijo — se calcula relativo al rango filtrado). Los delta
     * quedan en null cuando el periodo anterior no tiene base contra qué comparar
     * (evita división por cero / porcentajes sin sentido).
     */
    public record ComparacionAnterior(
        LocalDate fechaInicioAnterior,
        LocalDate fechaFinAnterior,
        BigDecimal gastoBrutoAnterior,
        BigDecimal gastoNetoAnterior,
        BigDecimal ahorroAnterior,
        BigDecimal deltaGastoBrutoPct,
        BigDecimal deltaGastoNetoPct,
        BigDecimal deltaAhorroPct
    ) {}
}
