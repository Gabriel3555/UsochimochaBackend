package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.application.dto.FuelDashboardResponse;
import com.app.usochicamochabackend.fuel.application.dto.FuelTrendResponse;
import com.app.usochicamochabackend.fuel.application.port.GetFuelDashboardUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelPurchaseEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;
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

    private final FuelPurchaseRepository fuelPurchaseRepository;
    private final RefuelingRecordsRepository refuelingRecordsRepository;

    @Override
    public FuelDashboardResponse obtenerDashboard(LocalDate fechaInicio, LocalDate fechaFin) {
        FechaRangoUtil.Rango rango = FechaRangoUtil.resolver(fechaInicio, fechaFin);

        BigDecimal totalComprasAlmacen = fuelPurchaseRepository.sumTotalCalculadoBetween(rango.inicio(), rango.fin());
        BigDecimal totalDescuentos = fuelPurchaseRepository.sumDescuentoBetween(rango.inicio(), rango.fin());
        BigDecimal totalTanqueosBomba = refuelingRecordsRepository.sumTotalCalculadoBombaBetween(rango.inicio(), rango.fin());

        BigDecimal gastoBruto = totalComprasAlmacen.add(totalTanqueosBomba);
        BigDecimal gastoNeto = gastoBruto.subtract(totalDescuentos);
        BigDecimal ahorro = totalDescuentos;

        List<FuelDashboardResponse.GalonesPorTipo> galonesPorTipo = agruparGalonesPorTipo(rango);
        List<FuelDashboardResponse.GastoPorTipo> gastoPorTipo = agruparGastoPorTipo(rango);

        long discrepancias = fuelPurchaseRepository.countByDiscrepanciaValorTrueAndFechaCompraBetween(rango.inicio(), rango.fin())
                + refuelingRecordsRepository.countByDiscrepanciaValorTrueAndFechaRegistroBetween(rango.inicio(), rango.fin());

        BigDecimal galonesComprados = fuelPurchaseRepository.sumCantidadBetween(rango.inicio(), rango.fin());
        BigDecimal precioPromedioGalonComprado = galonesComprados.compareTo(BigDecimal.ZERO) > 0
                ? totalComprasAlmacen.divide(galonesComprados, 2, RoundingMode.HALF_UP)
                : null;

        FuelDashboardResponse.ComparacionAnterior comparacionAnterior =
                calcularComparacionAnterior(rango, gastoBruto, gastoNeto, ahorro);

        return new FuelDashboardResponse(
                rango.fechaInicio(), rango.fechaFin(),
                totalComprasAlmacen, totalTanqueosBomba, totalDescuentos,
                gastoBruto, gastoNeto, ahorro,
                galonesPorTipo, gastoPorTipo,
                discrepancias, precioPromedioGalonComprado, comparacionAnterior);
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
        BigDecimal descuentoAnterior = fuelPurchaseRepository.sumDescuentoBetween(inicioAnteriorTs, finAnteriorTs);
        BigDecimal tanqueosBombaAnterior = refuelingRecordsRepository.sumTotalCalculadoBombaBetween(inicioAnteriorTs, finAnteriorTs);

        BigDecimal gastoBrutoAnterior = comprasAnterior.add(tanqueosBombaAnterior);
        BigDecimal gastoNetoAnterior = gastoBrutoAnterior.subtract(descuentoAnterior);
        BigDecimal ahorroAnterior = descuentoAnterior;

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
        Map<YearMonth, BigDecimal> galonesPorMes = new LinkedHashMap<>();
        for (RefuelingRecordsEntity tanqueo : tanqueos) {
            YearMonth mes = YearMonth.from(tanqueo.getFechaRegistro().toLocalDateTime());
            galonesPorMes.merge(mes, tanqueo.getCantidadGalones(), BigDecimal::add);
            if ("BOMBA".equals(tanqueo.getLugar()) && tanqueo.getTotalCalculado() != null) {
                tanqueosBombaPorMes.merge(mes, tanqueo.getTotalCalculado(), BigDecimal::add);
            }
        }

        List<FuelTrendResponse> tendencia = new ArrayList<>();
        for (YearMonth mes = mesInicio; !mes.isAfter(mesActual); mes = mes.plusMonths(1)) {
            BigDecimal gastoBrutoMes = comprasPorMes.getOrDefault(mes, BigDecimal.ZERO)
                    .add(tanqueosBombaPorMes.getOrDefault(mes, BigDecimal.ZERO));
            BigDecimal gastoNetoMes = gastoBrutoMes.subtract(descuentoPorMes.getOrDefault(mes, BigDecimal.ZERO));
            BigDecimal galonesMes = galonesPorMes.getOrDefault(mes, BigDecimal.ZERO);
            tendencia.add(new FuelTrendResponse(mes.atDay(1), gastoBrutoMes, gastoNetoMes, galonesMes));
        }
        return tendencia;
    }
}
