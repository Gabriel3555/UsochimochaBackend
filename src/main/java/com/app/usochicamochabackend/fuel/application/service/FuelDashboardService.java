package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.application.dto.FuelBudgetProjectionRow;
import com.app.usochicamochabackend.fuel.application.dto.FuelDashboardResponse;
import com.app.usochicamochabackend.fuel.application.dto.FuelTrendResponse;
import com.app.usochicamochabackend.fuel.application.port.GetFuelDashboardUseCase;
import com.app.usochicamochabackend.fuel.application.port.GetFuelPerformanceUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelPurchaseEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelMonthlyDiscountRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelPurchaseRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FuelDashboardService implements GetFuelDashboardUseCase {

    private static final int MESES_HISTORICOS_PROYECCION = 6;
    private static final int MESES_PROYECTADOS = 3;

    private static final List<String> TIPOS_RENDIMIENTO = List.of("MAQUINARIA", "VEHICULO", "MOTOCICLETA");

    private final FuelPurchaseRepository fuelPurchaseRepository;
    private final RefuelingRecordsRepository refuelingRecordsRepository;
    private final FuelMonthlyDiscountRepository fuelMonthlyDiscountRepository;
    private final GetFuelPerformanceUseCase getFuelPerformanceUseCase;

    @Override
    public FuelDashboardResponse obtenerDashboard(LocalDate fechaInicio, LocalDate fechaFin) {
        FechaRangoUtil.Rango rango = FechaRangoUtil.resolver(fechaInicio, fechaFin);

        BigDecimal totalComprasAlmacen = fuelPurchaseRepository.sumTotalCalculadoBetween(rango.inicio(), rango.fin());
        BigDecimal totalTanqueosBomba = refuelingRecordsRepository.sumTotalCalculadoBombaBetween(rango.inicio(), rango.fin());
        BigDecimal totalDescuentos = fuelPurchaseRepository.sumDescuentoBetween(rango.inicio(), rango.fin())
                .add(refuelingRecordsRepository.sumDescuentoBombaBetween(rango.inicio(), rango.fin()));

        // totalCalculado (compras y bomba) ya viene NETO de descuento (cantidad*precio -
        // descuento, ver FuelPurchaseService/RefuelingRecordService) — por eso "neto" es
        // la suma directa, y "bruto" se reconstruye sumando el descuento de vuelta. Restarlo
        // de nuevo aquí sería un doble descuento.
        BigDecimal gastoNetoSinDescuentoMensual = totalComprasAlmacen.add(totalTanqueosBomba);
        BigDecimal gastoBruto = gastoNetoSinDescuentoMensual.add(totalDescuentos);

        // Descuento mensual (V26): un rebate que el proveedor informa DESPUÉS de que
        // ya se registró el gasto, no está incorporado en ningún totalCalculado — se
        // resta aparte del gasto neto, sin tocar gastoBruto (que sigue representando
        // el total sin ningún descuento).
        BigDecimal descuentoMensual = fuelMonthlyDiscountRepository.sumMontoSolapado(rango.fechaInicio(), rango.fechaFin());
        BigDecimal gastoNeto = gastoNetoSinDescuentoMensual.subtract(descuentoMensual);
        BigDecimal ahorro = totalDescuentos.add(descuentoMensual);

        List<FuelDashboardResponse.GalonesPorTipo> galonesPorTipo = agruparGalonesPorTipo(rango);
        List<FuelDashboardResponse.GastoPorTipo> gastoPorTipo = agruparGastoPorTipo(rango);
        List<FuelDashboardResponse.GalonesPorTipo> galonesBombaPorTipo = agruparGalonesBombaPorTipo(rango);

        long discrepancias = fuelPurchaseRepository.countByDiscrepanciaValorTrueAndFechaCompraBetween(rango.inicio(), rango.fin())
                + refuelingRecordsRepository.countByDiscrepanciaValorTrueAndFechaRegistroBetween(rango.inicio(), rango.fin());
        long alertasRendimiento = contarAlertasRendimiento(rango.fechaInicio(), rango.fechaFin());

        BigDecimal galonesComprados = fuelPurchaseRepository.sumCantidadBetween(rango.inicio(), rango.fin());
        BigDecimal precioPromedioGalonComprado = galonesComprados.compareTo(BigDecimal.ZERO) > 0
                ? totalComprasAlmacen.divide(galonesComprados, 2, RoundingMode.HALF_UP)
                : null;

        FuelDashboardResponse.ComparacionAnterior comparacionAnterior =
                calcularComparacionAnterior(rango, gastoBruto, gastoNeto, ahorro);

        return new FuelDashboardResponse(
                rango.fechaInicio(), rango.fechaFin(),
                totalComprasAlmacen, totalTanqueosBomba, totalDescuentos, descuentoMensual,
                gastoBruto, gastoNeto, ahorro,
                galonesPorTipo, gastoPorTipo, galonesBombaPorTipo,
                discrepancias, alertasRendimiento, precioPromedioGalonComprado, comparacionAnterior);
    }

    // Reutiliza GetFuelPerformanceUseCase (misma fuente que la pantalla de Rendimiento
    // y su Excel) en vez de recalcular la lógica de alerta acá — un solo lugar decide
    // qué es una alerta de rendimiento (8% fijo / rango aprendido / techo del 13%, ver
    // FuelPerformanceService). Se recorren los 3 tipos porque obtenerRendimiento agrupa
    // por tipo, no hay un "TODOS" en ese endpoint.
    private long contarAlertasRendimiento(LocalDate fechaInicio, LocalDate fechaFin) {
        return TIPOS_RENDIMIENTO.stream()
                .flatMap(tipo -> getFuelPerformanceUseCase.obtenerRendimiento(tipo, fechaInicio, fechaFin).stream())
                .filter(fila -> Boolean.TRUE.equals(fila.alerta()))
                .count();
    }

    /**
     * Compara contra el periodo inmediatamente anterior, de la misma duración que el
     * filtrado — no "mes anterior" fijo. Si el usuario filtró 22 días, se compara contra
     * los 22 días previos al fechaInicio filtrado.
     */
    private FuelDashboardResponse.ComparacionAnterior calcularComparacionAnterior(
            FechaRangoUtil.Rango rango, BigDecimal gastoBruto, BigDecimal gastoNeto, BigDecimal ahorro) {
        long duracionDias = ChronoUnit.DAYS.between(rango.fechaInicio(), rango.fechaFin()) + 1;
        LocalDate fechaFinAnterior = rango.fechaInicio().minusDays(1);
        LocalDate fechaInicioAnterior = fechaFinAnterior.minusDays(duracionDias - 1);

        Timestamp inicioAnteriorTs = Timestamp.valueOf(LocalDateTime.of(fechaInicioAnterior, LocalTime.MIN));
        Timestamp finAnteriorTs = Timestamp.valueOf(LocalDateTime.of(fechaFinAnterior, LocalTime.MAX));

        BigDecimal comprasAnterior = fuelPurchaseRepository.sumTotalCalculadoBetween(inicioAnteriorTs, finAnteriorTs);
        BigDecimal tanqueosBombaAnterior = refuelingRecordsRepository.sumTotalCalculadoBombaBetween(inicioAnteriorTs, finAnteriorTs);
        BigDecimal descuentoAnterior = fuelPurchaseRepository.sumDescuentoBetween(inicioAnteriorTs, finAnteriorTs)
                .add(refuelingRecordsRepository.sumDescuentoBombaBetween(inicioAnteriorTs, finAnteriorTs));
        BigDecimal descuentoMensualAnterior = fuelMonthlyDiscountRepository.sumMontoSolapado(fechaInicioAnterior, fechaFinAnterior);

        BigDecimal gastoNetoAnteriorSinDescuentoMensual = comprasAnterior.add(tanqueosBombaAnterior);
        BigDecimal gastoBrutoAnterior = gastoNetoAnteriorSinDescuentoMensual.add(descuentoAnterior);
        BigDecimal gastoNetoAnterior = gastoNetoAnteriorSinDescuentoMensual.subtract(descuentoMensualAnterior);
        BigDecimal ahorroAnterior = descuentoAnterior.add(descuentoMensualAnterior);

        return new FuelDashboardResponse.ComparacionAnterior(
                fechaInicioAnterior, fechaFinAnterior,
                gastoBrutoAnterior, gastoNetoAnterior, ahorroAnterior,
                deltaPorcentual(gastoBruto, gastoBrutoAnterior),
                deltaPorcentual(gastoNeto, gastoNetoAnterior),
                deltaPorcentual(ahorro, ahorroAnterior));
    }

    /** % de cambio de actual vs anterior; null si el periodo anterior no tiene base (evita división por cero). */
    private BigDecimal deltaPorcentual(BigDecimal actual, BigDecimal anterior) {
        if (anterior == null || anterior.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return actual.subtract(anterior)
                .divide(anterior, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(1, RoundingMode.HALF_UP);
    }

    private List<FuelDashboardResponse.GalonesPorTipo> agruparGalonesPorTipo(FechaRangoUtil.Rango rango) {
        Map<Long, BigDecimal> galonesPorTipo = new LinkedHashMap<>();
        for (Object[] fila : refuelingRecordsRepository.sumCantidadPorTipoBetween(rango.inicio(), rango.fin())) {
            galonesPorTipo.put((Long) fila[0], (BigDecimal) fila[1]);
        }
        return galonesPorTipo.entrySet().stream()
                .map(entry -> new FuelDashboardResponse.GalonesPorTipo(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<FuelDashboardResponse.GalonesPorTipo> agruparGalonesBombaPorTipo(FechaRangoUtil.Rango rango) {
        Map<Long, BigDecimal> galonesBombaPorTipo = new LinkedHashMap<>();
        for (Object[] fila : refuelingRecordsRepository.sumCantidadBombaPorTipoBetween(rango.inicio(), rango.fin())) {
            galonesBombaPorTipo.put((Long) fila[0], (BigDecimal) fila[1]);
        }
        return galonesBombaPorTipo.entrySet().stream()
                .map(entry -> new FuelDashboardResponse.GalonesPorTipo(entry.getKey(), entry.getValue()))
                .toList();
    }

    /** Gasto total por tipo = compras de almacén + tanqueos en bomba (mismos dos orígenes que gastoBruto). */
    private List<FuelDashboardResponse.GastoPorTipo> agruparGastoPorTipo(FechaRangoUtil.Rango rango) {
        Map<Long, BigDecimal> gastoPorTipo = new LinkedHashMap<>();
        for (Object[] fila : fuelPurchaseRepository.sumTotalCalculadoPorTipoBetween(rango.inicio(), rango.fin())) {
            gastoPorTipo.merge((Long) fila[0], (BigDecimal) fila[1], BigDecimal::add);
        }
        for (Object[] fila : refuelingRecordsRepository.sumTotalCalculadoBombaPorTipoBetween(rango.inicio(), rango.fin())) {
            gastoPorTipo.merge((Long) fila[0], (BigDecimal) fila[1], BigDecimal::add);
        }
        return gastoPorTipo.entrySet().stream()
                .map(entry -> new FuelDashboardResponse.GastoPorTipo(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * Tendencia histórica mes a mes de los últimos {@code meses} (incluye el mes de
     * {@code fechaFin}). Termina en {@code fechaFin} si se especifica — no siempre "hoy" —
     * para poder alinearse con el rango que el usuario haya filtrado en el dashboard.
     * Agrupa en memoria en vez de con funciones SQL de fecha para no depender de que H2
     * (usado en tests) y Postgres (producción) truncen fechas igual.
     */
    @Override
    public List<FuelTrendResponse> obtenerTendencia(Integer meses, LocalDate fechaFin) {
        int n = meses != null && meses > 0 ? meses : 6;
        YearMonth mesActual = YearMonth.from(fechaFin != null ? fechaFin : LocalDate.now());
        YearMonth mesInicio = mesActual.minusMonths(n - 1L);

        Timestamp inicioTs = Timestamp.valueOf(LocalDateTime.of(mesInicio.atDay(1), LocalTime.MIN));
        Timestamp finTs = Timestamp.valueOf(LocalDateTime.of(mesActual.atEndOfMonth(), LocalTime.MAX));

        List<FuelPurchaseEntity> compras = fuelPurchaseRepository.findByFechaCompraBetweenOrderByFechaCompraDesc(inicioTs, finTs);
        List<RefuelingRecordsEntity> tanqueos = refuelingRecordsRepository.findByFechaRegistroBetween(inicioTs, finTs);

        Map<YearMonth, BigDecimal> comprasPorMes = new LinkedHashMap<>();
        Map<YearMonth, BigDecimal> descuentoPorMes = new LinkedHashMap<>();
        for (FuelPurchaseEntity compra : compras) {
            YearMonth mes = YearMonth.from(compra.getFechaCompra().toLocalDateTime());
            comprasPorMes.merge(mes, compra.getTotalCalculado(), BigDecimal::add);
            if (compra.getDescuento() != null) {
                descuentoPorMes.merge(mes, compra.getDescuento(), BigDecimal::add);
            }
        }

        Map<YearMonth, BigDecimal> tanqueosBombaPorMes = new LinkedHashMap<>();
        Map<YearMonth, BigDecimal> descuentoBombaPorMes = new LinkedHashMap<>();
        Map<YearMonth, BigDecimal> galonesPorMes = new LinkedHashMap<>();
        for (RefuelingRecordsEntity tanqueo : tanqueos) {
            YearMonth mes = YearMonth.from(tanqueo.getFechaRegistro().toLocalDateTime());
            galonesPorMes.merge(mes, tanqueo.getCantidadGalones(), BigDecimal::add);
            if ("BOMBA".equals(tanqueo.getLugar()) && tanqueo.getTotalCalculado() != null) {
                tanqueosBombaPorMes.merge(mes, tanqueo.getTotalCalculado(), BigDecimal::add);
                if (tanqueo.getDescuento() != null) {
                    descuentoBombaPorMes.merge(mes, tanqueo.getDescuento(), BigDecimal::add);
                }
            }
        }

        List<FuelTrendResponse> tendencia = new ArrayList<>();
        for (YearMonth mes = mesInicio; !mes.isAfter(mesActual); mes = mes.plusMonths(1)) {
            // Igual que en obtenerDashboard: los totales ya vienen netos de descuento, así
            // que "neto" es la suma directa y "bruto" se reconstruye sumando el descuento.
            BigDecimal gastoNetoMes = comprasPorMes.getOrDefault(mes, BigDecimal.ZERO)
                    .add(tanqueosBombaPorMes.getOrDefault(mes, BigDecimal.ZERO));
            BigDecimal descuentoMes = descuentoPorMes.getOrDefault(mes, BigDecimal.ZERO)
                    .add(descuentoBombaPorMes.getOrDefault(mes, BigDecimal.ZERO));
            BigDecimal gastoBrutoMes = gastoNetoMes.add(descuentoMes);
            BigDecimal galonesMes = galonesPorMes.getOrDefault(mes, BigDecimal.ZERO);
            tendencia.add(new FuelTrendResponse(mes.atDay(1), gastoBrutoMes, gastoNetoMes, galonesMes));
        }
        return tendencia;
    }

    /**
     * Proyección presupuestal: {@link #MESES_HISTORICOS_PROYECCION} meses históricos (todos se
     * devuelven, {@code proyectado=false}) + {@link #MESES_PROYECTADOS} meses proyectados con el
     * promedio del gasto neto de los meses <b>cerrados</b> (excluye el mes en curso, aún
     * incompleto) y <b>con actividad real</b> (excluye meses en $0 — de antes de que el sistema
     * tuviera datos, que sesgarían el promedio hacia abajo). Con esto el promedio se vuelve más
     * preciso a medida que pasan meses reales, sin necesidad de tocar esta lógica de nuevo.
     */
    @Override
    public List<FuelBudgetProjectionRow> obtenerProyeccionPresupuestal(LocalDate fechaFin) {
        List<FuelTrendResponse> tendencia = obtenerTendencia(MESES_HISTORICOS_PROYECCION, fechaFin);
        YearMonth mesActual = YearMonth.from(fechaFin != null ? fechaFin : LocalDate.now());

        List<FuelBudgetProjectionRow> filas = new ArrayList<>();
        for (FuelTrendResponse punto : tendencia) {
            filas.add(new FuelBudgetProjectionRow(punto.mes(), punto.gastoNeto(), false));
        }

        List<BigDecimal> mesesParaPromedio = tendencia.stream()
                .filter(p -> !YearMonth.from(p.mes()).equals(mesActual))
                .map(FuelTrendResponse::gastoNeto)
                .filter(g -> g.compareTo(BigDecimal.ZERO) > 0)
                .toList();

        BigDecimal promedio = mesesParaPromedio.isEmpty()
                ? BigDecimal.ZERO
                : mesesParaPromedio.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(mesesParaPromedio.size()), 2, RoundingMode.HALF_UP);

        for (int i = 1; i <= MESES_PROYECTADOS; i++) {
            filas.add(new FuelBudgetProjectionRow(mesActual.plusMonths(i).atDay(1), promedio, true));
        }
        return filas;
    }
}
