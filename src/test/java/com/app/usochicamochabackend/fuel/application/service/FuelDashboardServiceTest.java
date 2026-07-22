package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.application.dto.FuelDashboardResponse;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelPurchaseRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuelDashboardServiceTest {

    @Mock
    private FuelPurchaseRepository fuelPurchaseRepository;

    @Mock
    private RefuelingRecordsRepository refuelingRecordsRepository;

    @InjectMocks
    private FuelDashboardService fuelDashboardService;

    @Test
    void obtenerDashboard_CalculaGastoBrutoNetoYAhorroCorrectamente() {
        when(fuelPurchaseRepository.sumTotalCalculadoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(new BigDecimal("1000000"));
        when(fuelPurchaseRepository.sumDescuentoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(new BigDecimal("50000"));
        when(refuelingRecordsRepository.sumTotalCalculadoBombaBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(new BigDecimal("300000"));
        when(refuelingRecordsRepository.sumCantidadPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of(new Object[]{1L, new BigDecimal("120.000")}, new Object[]{3L, new BigDecimal("40.500")}));

        FuelDashboardResponse response = fuelDashboardService.obtenerDashboard(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertEquals(0, new BigDecimal("1300000").compareTo(response.gastoBruto()));
        assertEquals(0, new BigDecimal("1250000").compareTo(response.gastoNeto()));
        assertEquals(0, new BigDecimal("50000").compareTo(response.ahorro()));
        assertEquals(2, response.galonesPorTipo().size());
    }

    @Test
    void obtenerDashboard_ConFechasNulas_UsaMesActual() {
        when(fuelPurchaseRepository.sumTotalCalculadoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(fuelPurchaseRepository.sumDescuentoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(refuelingRecordsRepository.sumTotalCalculadoBombaBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(BigDecimal.ZERO);
        when(refuelingRecordsRepository.sumCantidadPorTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());

        FuelDashboardResponse response = fuelDashboardService.obtenerDashboard(null, null);

        assertEquals(LocalDate.now().withDayOfMonth(1), response.fechaInicio());
        assertEquals(LocalDate.now(), response.fechaFin());
    }
}
