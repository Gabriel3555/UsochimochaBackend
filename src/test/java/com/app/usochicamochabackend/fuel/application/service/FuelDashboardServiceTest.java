package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.application.dto.FuelBudgetProjectionRow;
import com.app.usochicamochabackend.fuel.application.dto.FuelDashboardResponse;
import com.app.usochicamochabackend.fuel.application.dto.FuelPerformanceResponse;
import com.app.usochicamochabackend.fuel.application.dto.FuelTrendResponse;
import com.app.usochicamochabackend.fuel.application.port.GetFuelPerformanceUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelPurchaseEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelMonthlyDiscountRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelPurchaseRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuelDashboardServiceTest {

    @Mock
    private FuelPurchaseRepository fuelPurchaseRepository;

    @Mock
    private RefuelingRecordsRepository refuelingRecordsRepository;

    @Mock
    private FuelMonthlyDiscountRepository fuelMonthlyDiscountRepository;

    @Mock
    private GetFuelPerformanceUseCase getFuelPerformanceUseCase;

    @InjectMocks
    private FuelDashboardService fuelDashboardService;

    // lenient(): solo lo usan los tests de obtenerDashboard() (vía
    // contarAlertasRendimiento) — los de obtenerTendencia/obtenerProyeccionPresupuestal
    // no lo tocan, y sin lenient() el modo estricto de Mockito los marcaría como
    // "stubbing innecesario" y fallaría.
    @BeforeEach
    void stubRendimientoVacioPorDefecto() {
        lenient().when(getFuelPerformanceUseCase.obtenerRendimiento(any(), any(), any())).thenReturn(List.of());
    }

    @Test
    void obtenerDashboard_CalculaGastoBrutoNetoYAhorroCorrectamente() {
        // totalCalculado (compras y bomba) ya viene NETO de descuento (cantidad*precio -
        // descuento, ver FuelPurchaseService/RefuelingRecordService) — por eso "neto" se arma
        // sumando esos totales directamente, y "bruto" se reconstruye sumándole el descuento
        // de vuelta (bruto = neto + descuento), no restándolo (restar de nuevo sería doble
        // descuento).
        when(fuelPurchaseRepository.sumTotalCalculadoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(new BigDecimal("1000000"));
        when(fuelPurchaseRepository.sumDescuentoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(new BigDecimal("50000"));
        when(refuelingRecordsRepository.sumTotalCalculadoBombaBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(new BigDecimal("300000"));
        when(refuelingRecordsRepository.sumDescuentoBombaBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(fuelMonthlyDiscountRepository.sumMontoSolapado(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);
        when(refuelingRecordsRepository.sumCantidadPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(new Object[]{1L, new BigDecimal("120.000")}, new Object[]{3L, new BigDecimal("40.500")}));
        when(fuelPurchaseRepository.sumTotalCalculadoPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());
        when(refuelingRecordsRepository.sumTotalCalculadoBombaPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());
        when(refuelingRecordsRepository.sumCantidadBombaPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());
        when(fuelPurchaseRepository.sumCantidadBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(fuelPurchaseRepository.countByDiscrepanciaValorTrueAndFechaCompraBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(0L);
        when(refuelingRecordsRepository.countByDiscrepanciaValorTrueAndFechaRegistroBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(0L);

        FuelDashboardResponse response = fuelDashboardService.obtenerDashboard(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertEquals(0, new BigDecimal("1350000").compareTo(response.gastoBruto()));
        assertEquals(0, new BigDecimal("1300000").compareTo(response.gastoNeto()));
        assertEquals(0, new BigDecimal("50000").compareTo(response.ahorro()));
        assertEquals(2, response.galonesPorTipo().size());
    }

    @Test
    void obtenerDashboard_CuentaAlertasDeRendimientoDeLosTresTiposDeActivo() {
        when(fuelPurchaseRepository.sumTotalCalculadoBetween(any(Timestamp.class), any(Timestamp.class))).thenReturn(BigDecimal.ZERO);
        when(fuelPurchaseRepository.sumDescuentoBetween(any(Timestamp.class), any(Timestamp.class))).thenReturn(BigDecimal.ZERO);
        when(refuelingRecordsRepository.sumTotalCalculadoBombaBetween(any(Timestamp.class), any(Timestamp.class))).thenReturn(BigDecimal.ZERO);
        when(refuelingRecordsRepository.sumDescuentoBombaBetween(any(Timestamp.class), any(Timestamp.class))).thenReturn(BigDecimal.ZERO);
        when(fuelMonthlyDiscountRepository.sumMontoSolapado(any(LocalDate.class), any(LocalDate.class))).thenReturn(BigDecimal.ZERO);
        when(refuelingRecordsRepository.sumCantidadPorTipoBetween(any(Timestamp.class), any(Timestamp.class))).thenReturn(List.of());
        when(fuelPurchaseRepository.sumTotalCalculadoPorTipoBetween(any(Timestamp.class), any(Timestamp.class))).thenReturn(List.of());
        when(refuelingRecordsRepository.sumTotalCalculadoBombaPorTipoBetween(any(Timestamp.class), any(Timestamp.class))).thenReturn(List.of());
        when(refuelingRecordsRepository.sumCantidadBombaPorTipoBetween(any(Timestamp.class), any(Timestamp.class))).thenReturn(List.of());
        when(fuelPurchaseRepository.sumCantidadBetween(any(Timestamp.class), any(Timestamp.class))).thenReturn(BigDecimal.ZERO);
        when(fuelPurchaseRepository.countByDiscrepanciaValorTrueAndFechaCompraBetween(any(Timestamp.class), any(Timestamp.class))).thenReturn(0L);
        when(refuelingRecordsRepository.countByDiscrepanciaValorTrueAndFechaRegistroBetween(any(Timestamp.class), any(Timestamp.class))).thenReturn(0L);

        // 2 alertas en MAQUINARIA (una fila sin alerta no debe contar), 1 en VEHICULO, 0 en MOTOCICLETA.
        when(getFuelPerformanceUseCase.obtenerRendimiento(eq("MAQUINARIA"), any(), any()))
                .thenReturn(List.of(filaRendimiento(true), filaRendimiento(true), filaRendimiento(false)));
        when(getFuelPerformanceUseCase.obtenerRendimiento(eq("VEHICULO"), any(), any()))
                .thenReturn(List.of(filaRendimiento(true)));
        when(getFuelPerformanceUseCase.obtenerRendimiento(eq("MOTOCICLETA"), any(), any()))
                .thenReturn(List.of());

        FuelDashboardResponse response = fuelDashboardService.obtenerDashboard(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertEquals(3L, response.alertasRendimiento());
    }

    private FuelPerformanceResponse filaRendimiento(boolean alerta) {
        return new FuelPerformanceResponse(1L, 1, null, 1L, LocalDateTime.now(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, alerta, false, "Activo", false);
    }

    @Test
    void obtenerDashboard_DescuentoEnTanqueoBomba_SeSumaAlAhorro_BugCorregido() {
        // Antes de este fix, un descuento aplicado en un tanqueo de BOMBA se restaba en
        // silencio dentro de totalCalculado pero jamás se sumaba a "ahorro" (que solo miraba
        // fuel_purchases) — la tarjeta "Ahorro por descuentos" mostraba $0 aunque hubiera
        // descuentos reales en bomba.
        when(fuelPurchaseRepository.sumTotalCalculadoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(fuelPurchaseRepository.sumDescuentoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(refuelingRecordsRepository.sumTotalCalculadoBombaBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(new BigDecimal("950000"));
        when(refuelingRecordsRepository.sumDescuentoBombaBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(new BigDecimal("50000"));
        when(fuelMonthlyDiscountRepository.sumMontoSolapado(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);
        when(refuelingRecordsRepository.sumCantidadPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());
        when(fuelPurchaseRepository.sumTotalCalculadoPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());
        when(refuelingRecordsRepository.sumTotalCalculadoBombaPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());
        when(refuelingRecordsRepository.sumCantidadBombaPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());
        when(fuelPurchaseRepository.sumCantidadBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(fuelPurchaseRepository.countByDiscrepanciaValorTrueAndFechaCompraBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(0L);
        when(refuelingRecordsRepository.countByDiscrepanciaValorTrueAndFechaRegistroBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(0L);

        FuelDashboardResponse response = fuelDashboardService.obtenerDashboard(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertEquals(0, new BigDecimal("50000").compareTo(response.ahorro()));
        assertEquals(0, new BigDecimal("1000000").compareTo(response.gastoBruto()));
        assertEquals(0, new BigDecimal("950000").compareTo(response.gastoNeto()));
    }

    @Test
    void obtenerDashboard_ConDescuentoMensualSolapado_SeRestaDeGastoNetoYSumaAlAhorro() {
        // El descuento mensual (V26) es un rebate que llega DESPUÉS de que ya se
        // registró el gasto (no viene de ningún totalCalculado individual): debe
        // restarse del gasto neto y sumarse al ahorro, sin tocar el gasto bruto.
        when(fuelPurchaseRepository.sumTotalCalculadoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(new BigDecimal("1000000"));
        when(fuelPurchaseRepository.sumDescuentoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(refuelingRecordsRepository.sumTotalCalculadoBombaBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(refuelingRecordsRepository.sumDescuentoBombaBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(fuelMonthlyDiscountRepository.sumMontoSolapado(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("80000"));
        when(refuelingRecordsRepository.sumCantidadPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());
        when(fuelPurchaseRepository.sumTotalCalculadoPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());
        when(refuelingRecordsRepository.sumTotalCalculadoBombaPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());
        when(refuelingRecordsRepository.sumCantidadBombaPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());
        when(fuelPurchaseRepository.sumCantidadBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(fuelPurchaseRepository.countByDiscrepanciaValorTrueAndFechaCompraBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(0L);
        when(refuelingRecordsRepository.countByDiscrepanciaValorTrueAndFechaRegistroBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(0L);

        FuelDashboardResponse response = fuelDashboardService.obtenerDashboard(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertEquals(0, new BigDecimal("80000").compareTo(response.descuentoMensual()));
        assertEquals(0, new BigDecimal("1000000").compareTo(response.gastoBruto()));
        assertEquals(0, new BigDecimal("920000").compareTo(response.gastoNeto()));
        assertEquals(0, new BigDecimal("80000").compareTo(response.ahorro()));
    }

    @Test
    void obtenerDashboard_GastoPorTipoSumaComprasYTanqueosBombaDelMismoTipo() {
        when(fuelPurchaseRepository.sumTotalCalculadoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(fuelPurchaseRepository.sumDescuentoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(refuelingRecordsRepository.sumTotalCalculadoBombaBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(refuelingRecordsRepository.sumDescuentoBombaBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(fuelMonthlyDiscountRepository.sumMontoSolapado(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);
        when(refuelingRecordsRepository.sumCantidadPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());
        // Tipo 1 (ACPM): 700.000 en compras + 300.000 en tanqueos bomba = 1.000.000
        // Tipo 2 (CORRIENTE): solo 200.000 en tanqueos bomba, sin compras en el periodo
        when(fuelPurchaseRepository.sumTotalCalculadoPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(Collections.singletonList(new Object[]{1L, new BigDecimal("700000")}));
        when(refuelingRecordsRepository.sumTotalCalculadoBombaPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(new Object[]{1L, new BigDecimal("300000")}, new Object[]{2L, new BigDecimal("200000")}));
        when(refuelingRecordsRepository.sumCantidadBombaPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());
        when(fuelPurchaseRepository.sumCantidadBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(fuelPurchaseRepository.countByDiscrepanciaValorTrueAndFechaCompraBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(0L);
        when(refuelingRecordsRepository.countByDiscrepanciaValorTrueAndFechaRegistroBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(0L);

        FuelDashboardResponse response = fuelDashboardService.obtenerDashboard(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertEquals(2, response.gastoPorTipo().size());
        var tipo1 = response.gastoPorTipo().stream().filter(g -> g.fuelTypeId().equals(1L)).findFirst().orElseThrow();
        var tipo2 = response.gastoPorTipo().stream().filter(g -> g.fuelTypeId().equals(2L)).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("1000000").compareTo(tipo1.monto()));
        assertEquals(0, new BigDecimal("200000").compareTo(tipo2.monto()));
    }

    @Test
    void obtenerDashboard_GalonesBombaPorTipoExcluyeAlmacen_ADiferenciaDeGalonesPorTipo() {
        when(fuelPurchaseRepository.sumTotalCalculadoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(fuelPurchaseRepository.sumDescuentoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(refuelingRecordsRepository.sumTotalCalculadoBombaBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(refuelingRecordsRepository.sumDescuentoBombaBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(fuelMonthlyDiscountRepository.sumMontoSolapado(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);
        // galonesPorTipo (bomba+almacén combinados): 100 gal del tipo 1
        when(refuelingRecordsRepository.sumCantidadPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(Collections.singletonList(new Object[]{1L, new BigDecimal("100.000")}));
        when(fuelPurchaseRepository.sumTotalCalculadoPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());
        when(refuelingRecordsRepository.sumTotalCalculadoBombaPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());
        // galonesBombaPorTipo (solo bomba): apenas 30 de esos 100 gal fueron en bomba —
        // los otros 70 fueron ALMACEN, sin costo, y no deben contar en el precio/unidad.
        when(refuelingRecordsRepository.sumCantidadBombaPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(Collections.singletonList(new Object[]{1L, new BigDecimal("30.000")}));
        when(fuelPurchaseRepository.sumCantidadBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(fuelPurchaseRepository.countByDiscrepanciaValorTrueAndFechaCompraBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(0L);
        when(refuelingRecordsRepository.countByDiscrepanciaValorTrueAndFechaRegistroBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(0L);

        FuelDashboardResponse response = fuelDashboardService.obtenerDashboard(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertEquals(0, new BigDecimal("100.000").compareTo(response.galonesPorTipo().get(0).cantidad()));
        assertEquals(1, response.galonesBombaPorTipo().size());
        assertEquals(0, new BigDecimal("30.000").compareTo(response.galonesBombaPorTipo().get(0).cantidad()));
    }

    @Test
    void obtenerDashboard_ConFechasNulas_UsaMesActual() {
        when(fuelPurchaseRepository.sumTotalCalculadoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(fuelPurchaseRepository.sumDescuentoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(refuelingRecordsRepository.sumTotalCalculadoBombaBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(refuelingRecordsRepository.sumDescuentoBombaBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(fuelMonthlyDiscountRepository.sumMontoSolapado(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);
        when(refuelingRecordsRepository.sumCantidadPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());
        when(fuelPurchaseRepository.sumTotalCalculadoPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());
        when(refuelingRecordsRepository.sumTotalCalculadoBombaPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());
        when(refuelingRecordsRepository.sumCantidadBombaPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());
        when(fuelPurchaseRepository.sumCantidadBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(fuelPurchaseRepository.countByDiscrepanciaValorTrueAndFechaCompraBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(0L);
        when(refuelingRecordsRepository.countByDiscrepanciaValorTrueAndFechaRegistroBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(0L);

        FuelDashboardResponse response = fuelDashboardService.obtenerDashboard(null, null);

        assertEquals(LocalDate.now().withDayOfMonth(1), response.fechaInicio());
        assertEquals(LocalDate.now(), response.fechaFin());
    }

    @Test
    void obtenerDashboard_CalculaDeltaVsPeriodoAnteriorDeIgualDuracion() {
        LocalDate fechaInicio = LocalDate.of(2026, 7, 1);
        LocalDate fechaFin = LocalDate.of(2026, 7, 10); // 10 días -> anterior: 21-30 jun (también 10 días)

        when(fuelPurchaseRepository.sumTotalCalculadoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenAnswer(inv -> esPeriodoActual(inv.getArgument(0), fechaInicio)
                        ? new BigDecimal("1100000") : new BigDecimal("1000000"));
        when(fuelPurchaseRepository.sumDescuentoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(refuelingRecordsRepository.sumTotalCalculadoBombaBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(refuelingRecordsRepository.sumDescuentoBombaBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(fuelMonthlyDiscountRepository.sumMontoSolapado(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);
        when(refuelingRecordsRepository.sumCantidadPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());
        when(fuelPurchaseRepository.sumTotalCalculadoPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());
        when(refuelingRecordsRepository.sumTotalCalculadoBombaPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());
        when(refuelingRecordsRepository.sumCantidadBombaPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());
        when(fuelPurchaseRepository.sumCantidadBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(fuelPurchaseRepository.countByDiscrepanciaValorTrueAndFechaCompraBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(2L);
        when(refuelingRecordsRepository.countByDiscrepanciaValorTrueAndFechaRegistroBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(1L);

        FuelDashboardResponse response = fuelDashboardService.obtenerDashboard(fechaInicio, fechaFin);

        assertEquals(3L, response.discrepancias());
        assertEquals(LocalDate.of(2026, 6, 21), response.comparacionAnterior().fechaInicioAnterior());
        assertEquals(LocalDate.of(2026, 6, 30), response.comparacionAnterior().fechaFinAnterior());
        assertEquals(0, new BigDecimal("1000000").compareTo(response.comparacionAnterior().gastoBrutoAnterior()));
        // (1.100.000 - 1.000.000) / 1.000.000 * 100 = 10.0%
        assertEquals(0, new BigDecimal("10.0").compareTo(response.comparacionAnterior().deltaGastoBrutoPct()));
    }

    private boolean esPeriodoActual(Timestamp inicio, LocalDate fechaInicioActual) {
        return inicio.toLocalDateTime().toLocalDate().equals(fechaInicioActual);
    }

    @Test
    void obtenerTendencia_AgrupaPorMesYRellenaMesesSinDatosConCero() {
        YearMonth mesActual = YearMonth.now();
        YearMonth hace1Mes = mesActual.minusMonths(1);
        YearMonth hace2Meses = mesActual.minusMonths(2);

        FuelPurchaseEntity compraHace2Meses = FuelPurchaseEntity.builder()
                .fechaCompra(Timestamp.valueOf(LocalDateTime.of(hace2Meses.atDay(10), java.time.LocalTime.NOON)))
                .totalCalculado(new BigDecimal("500000"))
                .descuento(new BigDecimal("50000"))
                .build();

        RefuelingRecordsEntity tanqueoBombaHace1Mes = RefuelingRecordsEntity.builder()
                .fechaRegistro(Timestamp.valueOf(LocalDateTime.of(hace1Mes.atDay(5), java.time.LocalTime.NOON)))
                .lugar("BOMBA")
                .totalCalculado(new BigDecimal("200000"))
                .cantidadGalones(new BigDecimal("30"))
                .build();
        RefuelingRecordsEntity tanqueoAlmacenMesActual = RefuelingRecordsEntity.builder()
                .fechaRegistro(Timestamp.valueOf(LocalDateTime.of(mesActual.atDay(1), java.time.LocalTime.NOON)))
                .lugar("ALMACEN")
                .cantidadGalones(new BigDecimal("10"))
                .build();

        when(fuelPurchaseRepository.findByFechaCompraBetweenOrderByFechaCompraDesc(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(compraHace2Meses));
        when(refuelingRecordsRepository.findByFechaRegistroBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(tanqueoBombaHace1Mes, tanqueoAlmacenMesActual));

        List<FuelTrendResponse> tendencia = fuelDashboardService.obtenerTendencia(3, null);

        assertEquals(3, tendencia.size());
        // Orden cronológico: hace 2 meses -> hace 1 mes -> mes actual.
        assertEquals(hace2Meses.atDay(1), tendencia.get(0).mes());
        // compra con totalCalculado=500000 (ya neto) + descuento=50000 -> bruto=550000, neto=500000
        assertEquals(0, new BigDecimal("550000").compareTo(tendencia.get(0).gastoBruto()));
        assertEquals(0, new BigDecimal("500000").compareTo(tendencia.get(0).gastoNeto()));
        assertEquals(0, BigDecimal.ZERO.compareTo(tendencia.get(0).galonesTotal()));

        assertEquals(hace1Mes.atDay(1), tendencia.get(1).mes());
        assertEquals(0, new BigDecimal("200000").compareTo(tendencia.get(1).gastoBruto()));
        assertEquals(0, new BigDecimal("30").compareTo(tendencia.get(1).galonesTotal()));

        assertEquals(mesActual.atDay(1), tendencia.get(2).mes());
        assertEquals(0, BigDecimal.ZERO.compareTo(tendencia.get(2).gastoBruto())); // ALMACEN no valoriza
        assertEquals(0, new BigDecimal("10").compareTo(tendencia.get(2).galonesTotal()));
    }

    @Test
    void obtenerTendencia_SinMesesEspecificado_UsaSeisPorDefecto() {
        when(fuelPurchaseRepository.findByFechaCompraBetweenOrderByFechaCompraDesc(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());
        when(refuelingRecordsRepository.findByFechaRegistroBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());

        List<FuelTrendResponse> tendencia = fuelDashboardService.obtenerTendencia(null, null);

        assertEquals(6, tendencia.size());
    }

    @Test
    void obtenerTendencia_ConFechaFin_TerminaEnEsaFechaYNoEnHoy() {
        // fechaFin en el pasado (no "hoy") -> la tendencia de 3 meses debe terminar ahí,
        // para poder alinearse con un rango histórico largo filtrado por el usuario.
        LocalDate fechaFin = LocalDate.of(2024, 3, 15);
        YearMonth mesFin = YearMonth.of(2024, 3);
        YearMonth mesInicio = mesFin.minusMonths(2);

        when(fuelPurchaseRepository.findByFechaCompraBetweenOrderByFechaCompraDesc(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());
        when(refuelingRecordsRepository.findByFechaRegistroBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());

        List<FuelTrendResponse> tendencia = fuelDashboardService.obtenerTendencia(3, fechaFin);

        assertEquals(3, tendencia.size());
        assertEquals(mesInicio.atDay(1), tendencia.get(0).mes());
        assertEquals(mesFin.atDay(1), tendencia.get(2).mes());
    }

    private FuelPurchaseEntity compraEnMes(YearMonth mes, BigDecimal totalCalculado) {
        return FuelPurchaseEntity.builder()
                .fechaCompra(Timestamp.valueOf(LocalDateTime.of(mes.atDay(10), java.time.LocalTime.NOON)))
                .totalCalculado(totalCalculado)
                .build();
    }

    private RefuelingRecordsEntity tanqueoBombaEnMes(YearMonth mes, BigDecimal totalCalculado) {
        return RefuelingRecordsEntity.builder()
                .fechaRegistro(Timestamp.valueOf(LocalDateTime.of(mes.atDay(10), java.time.LocalTime.NOON)))
                .lugar("BOMBA")
                .totalCalculado(totalCalculado)
                .cantidadGalones(BigDecimal.ZERO)
                .build();
    }

    @Test
    void obtenerProyeccionPresupuestal_ExcluyeMesesEnCeroDelPromedio() {
        LocalDate fechaFin = LocalDate.of(2026, 8, 15);
        YearMonth junio = YearMonth.of(2026, 6);
        YearMonth julio = YearMonth.of(2026, 7);
        // marzo-mayo y agosto quedan sin ningún registro -> gastoNeto=0 en esos meses.

        when(fuelPurchaseRepository.findByFechaCompraBetweenOrderByFechaCompraDesc(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(Collections.emptyList());
        when(refuelingRecordsRepository.findByFechaRegistroBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(
                        tanqueoBombaEnMes(junio, new BigDecimal("200000")),
                        tanqueoBombaEnMes(julio, new BigDecimal("400000"))));

        List<FuelBudgetProjectionRow> proyeccion = fuelDashboardService.obtenerProyeccionPresupuestal(fechaFin);

        // 6 históricos (mar-ago) + 3 proyectados (sep-nov) = 9 filas.
        assertEquals(9, proyeccion.size());
        List<FuelBudgetProjectionRow> proyectadas = proyeccion.stream().filter(FuelBudgetProjectionRow::proyectado).toList();
        assertEquals(3, proyectadas.size());
        // Promedio = (200000 + 400000) / 2 = 300000 -> marzo/abril/mayo/agosto en $0 no cuentan.
        for (FuelBudgetProjectionRow fila : proyectadas) {
            assertEquals(0, new BigDecimal("300000").compareTo(fila.gastoNeto()));
        }
        assertEquals(YearMonth.of(2026, 9).atDay(1), proyectadas.get(0).mes());
        assertEquals(YearMonth.of(2026, 11).atDay(1), proyectadas.get(2).mes());
    }

    @Test
    void obtenerProyeccionPresupuestal_ExcluyeElMesEnCursoAunqueTengaActividad() {
        LocalDate fechaFin = LocalDate.of(2026, 8, 15);
        YearMonth julio = YearMonth.of(2026, 7);
        YearMonth agosto = YearMonth.of(2026, 8);

        when(fuelPurchaseRepository.findByFechaCompraBetweenOrderByFechaCompraDesc(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(Collections.emptyList());
        when(refuelingRecordsRepository.findByFechaRegistroBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(
                        tanqueoBombaEnMes(julio, new BigDecimal("200000")),
                        tanqueoBombaEnMes(agosto, new BigDecimal("999999")))); // mes en curso, con actividad real

        List<FuelBudgetProjectionRow> proyeccion = fuelDashboardService.obtenerProyeccionPresupuestal(fechaFin);

        // Si agosto (mes en curso) contara, el promedio sería (200000+999999)/2. Debe ser solo julio.
        BigDecimal promedioProyectado = proyeccion.stream().filter(FuelBudgetProjectionRow::proyectado)
                .findFirst().orElseThrow().gastoNeto();
        assertEquals(0, new BigDecimal("200000").compareTo(promedioProyectado));
    }

    @Test
    void obtenerProyeccionPresupuestal_TodoExcluido_PromedioCeroSinExcepcion() {
        LocalDate fechaFin = LocalDate.of(2026, 8, 15);

        when(fuelPurchaseRepository.findByFechaCompraBetweenOrderByFechaCompraDesc(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(Collections.emptyList());
        when(refuelingRecordsRepository.findByFechaRegistroBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(Collections.emptyList());

        List<FuelBudgetProjectionRow> proyeccion = fuelDashboardService.obtenerProyeccionPresupuestal(fechaFin);

        proyeccion.stream().filter(FuelBudgetProjectionRow::proyectado)
                .forEach(fila -> assertEquals(0, BigDecimal.ZERO.compareTo(fila.gastoNeto())));
    }

    @Test
    void obtenerProyeccionPresupuestal_LasFilasHistoricasNoSeFiltranDelResultado() {
        LocalDate fechaFin = LocalDate.of(2026, 8, 15);

        when(fuelPurchaseRepository.findByFechaCompraBetweenOrderByFechaCompraDesc(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(Collections.emptyList());
        when(refuelingRecordsRepository.findByFechaRegistroBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(Collections.emptyList());

        List<FuelBudgetProjectionRow> proyeccion = fuelDashboardService.obtenerProyeccionPresupuestal(fechaFin);

        List<FuelBudgetProjectionRow> historicas = proyeccion.stream().filter(f -> !f.proyectado()).toList();
        assertEquals(6, historicas.size());
        assertEquals(YearMonth.of(2026, 3).atDay(1), historicas.get(0).mes());
        assertEquals(YearMonth.of(2026, 8).atDay(1), historicas.get(5).mes());
    }
}
