package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuelPriceAnomalyServiceTest {

    @Mock
    private RefuelingRecordsRepository refuelingRecordsRepository;

    private FuelPriceAnomalyService fuelPriceAnomalyService;

    @BeforeEach
    void setUp() {
        fuelPriceAnomalyService = new FuelPriceAnomalyService(refuelingRecordsRepository);
        ReflectionTestUtils.setField(fuelPriceAnomalyService, "ventanaDias", 30);
        ReflectionTestUtils.setField(fuelPriceAnomalyService, "tolerancia", new BigDecimal("0.30"));
    }

    @Test
    void precioMuyPorEncimaDelPromedioReciente_DevuelveTrue() {
        when(refuelingRecordsRepository.avgPrecioUnitarioBombaRecienteByFuelType(eq(1L), any(), isNull()))
                .thenReturn(new BigDecimal("10000"));

        // 10000 * 1.30 = 13000 -> 14000 supera el 30% de tolerancia.
        assertTrue(fuelPriceAnomalyService.precioFueraDeRango(1L, new BigDecimal("14000"), null));
    }

    @Test
    void precioDentroDeLaToleranciaDelPromedio_DevuelveFalse() {
        when(refuelingRecordsRepository.avgPrecioUnitarioBombaRecienteByFuelType(eq(1L), any(), isNull()))
                .thenReturn(new BigDecimal("10000"));

        assertFalse(fuelPriceAnomalyService.precioFueraDeRango(1L, new BigDecimal("11000"), null));
    }

    @Test
    void precioMuyPorDebajoDelPromedioReciente_DevuelveTrue() {
        when(refuelingRecordsRepository.avgPrecioUnitarioBombaRecienteByFuelType(eq(1L), any(), isNull()))
                .thenReturn(new BigDecimal("10000"));

        // Un precio muy bajo también es sospechoso (dígito de menos), no solo el alto.
        assertTrue(fuelPriceAnomalyService.precioFueraDeRango(1L, new BigDecimal("1000"), null));
    }

    @Test
    void sinHistorialReciente_DevuelveFalseSinLanzarError() {
        when(refuelingRecordsRepository.avgPrecioUnitarioBombaRecienteByFuelType(eq(1L), any(), isNull()))
                .thenReturn(null);

        assertFalse(fuelPriceAnomalyService.precioFueraDeRango(1L, new BigDecimal("14000"), null));
    }

    @Test
    void precioUnitarioNulo_DevuelveFalseSinConsultarRepositorio() {
        assertFalse(fuelPriceAnomalyService.precioFueraDeRango(1L, null, null));

        verifyNoInteractions(refuelingRecordsRepository);
    }

    @Test
    void alEditar_PropagaElIdAExcluirAlRepositorio() {
        when(refuelingRecordsRepository.avgPrecioUnitarioBombaRecienteByFuelType(eq(1L), any(), eq(99L)))
                .thenReturn(new BigDecimal("10000"));

        fuelPriceAnomalyService.precioFueraDeRango(1L, new BigDecimal("10500"), 99L);

        verify(refuelingRecordsRepository).avgPrecioUnitarioBombaRecienteByFuelType(eq(1L), any(), eq(99L));
    }
}
