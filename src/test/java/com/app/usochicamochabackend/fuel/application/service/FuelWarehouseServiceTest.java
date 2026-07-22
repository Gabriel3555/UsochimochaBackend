package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.application.dto.FuelWarehouseMovementsResponse;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelInventoryEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelInventoryRepository;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuelWarehouseServiceTest {

    @Mock
    private FuelInventoryRepository fuelInventoryRepository;

    @Mock
    private FuelPurchaseRepository fuelPurchaseRepository;

    @Mock
    private RefuelingRecordsRepository refuelingRecordsRepository;

    @InjectMocks
    private FuelWarehouseService fuelWarehouseService;

    @Test
    void obtenerMovimientos_CalculaSaldoInicialAPartirDelSaldoActualYMovimientosDelPeriodo() {
        // saldo actual 70; entradas 100; salidas 30 -> saldoInicial = 70 - 100 + 30 = 0
        when(fuelInventoryRepository.findAll()).thenReturn(List.of(
                FuelInventoryEntity.builder().areaCosto("DISTRITO").fuelTypeId(1L).cantidadDisponible(new BigDecimal("70")).build()));
        when(fuelPurchaseRepository.sumCantidadPorAreaYTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(Collections.singletonList(new Object[]{"DISTRITO", 1L, new BigDecimal("100")}));
        when(refuelingRecordsRepository.sumCantidadAlmacenPorAreaYTipoBetween(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(Collections.singletonList(new Object[]{"DISTRITO", 1L, new BigDecimal("30")}));
        when(fuelPurchaseRepository.findByFechaCompraBetweenOrderByFechaCompraDesc(any(Timestamp.class), any(Timestamp.class)))
                .thenReturn(List.of());

        FuelWarehouseMovementsResponse response = fuelWarehouseService.obtenerMovimientos(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertEquals(1, response.conciliacion().size());
        FuelWarehouseMovementsResponse.Conciliacion fila = response.conciliacion().get(0);
        assertEquals(0, BigDecimal.ZERO.compareTo(fila.saldoInicial()));
        assertEquals(0, new BigDecimal("100").compareTo(fila.entradas()));
        assertEquals(0, new BigDecimal("30").compareTo(fila.salidas()));
        assertEquals(0, new BigDecimal("70").compareTo(fila.saldoFinal()));
    }
}
