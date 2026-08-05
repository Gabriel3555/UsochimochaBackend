package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.infrastructure.entity.AssetFuelConfigEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.AssetFuelConfigRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuelFullConsistencyServiceTest {

    @Mock
    private RefuelingRecordsRepository refuelingRecordsRepository;

    @Mock
    private AssetFuelConfigRepository assetFuelConfigRepository;

    private FuelFullConsistencyService fuelFullConsistencyService;

    private final Timestamp fecha = Timestamp.valueOf(LocalDateTime.of(2026, 8, 5, 10, 0));

    @BeforeEach
    void setUp() {
        fuelFullConsistencyService = new FuelFullConsistencyService(refuelingRecordsRepository, assetFuelConfigRepository);
        ReflectionTestUtils.setField(fuelFullConsistencyService, "tolerancia", new BigDecimal("0.15"));
    }

    private RefuelingRecordsEntity anteriorVehiculo(BigDecimal horometroKm) {
        return RefuelingRecordsEntity.builder().id(1L).vehicleId(5).horometroKm(horometroKm).build();
    }

    @Test
    void esFullYCantidadCoincideConLoProyectado_DevuelveFalse() {
        when(refuelingRecordsRepository.findAnteriorPorVehicleId(5, fecha)).thenReturn(Optional.of(anteriorVehiculo(new BigDecimal("47498"))));
        when(assetFuelConfigRepository.findByVehicleId(5)).thenReturn(Optional.of(
                AssetFuelConfigEntity.builder().vehicleId(5).consumoEstandar(new BigDecimal("11")).unidadConsumo("KM_POR_GALON").build()));

        // 182 km recorridos / 11 km-por-gal = ~16.5 gal proyectados; tanqueó 17 -> consistente.
        boolean resultado = fuelFullConsistencyService.fullInconsistente(
                5, null, true, new BigDecimal("17"), new BigDecimal("47680"), fecha, null);

        assertFalse(resultado);
    }

    @Test
    void esFullPeroCantidadMuyPorDebajoDeLoProyectado_DevuelveTrue() {
        when(refuelingRecordsRepository.findAnteriorPorVehicleId(5, fecha)).thenReturn(Optional.of(anteriorVehiculo(new BigDecimal("47498"))));
        when(assetFuelConfigRepository.findByVehicleId(5)).thenReturn(Optional.of(
                AssetFuelConfigEntity.builder().vehicleId(5).consumoEstandar(new BigDecimal("11")).unidadConsumo("KM_POR_GALON").build()));

        // Mismos 182 km -> ~16.5 gal proyectados, pero dijo que llenó con solo 5 gal:
        // o no llenó de verdad, o el consumo real fue mucho menor al estándar.
        boolean resultado = fuelFullConsistencyService.fullInconsistente(
                5, null, true, new BigDecimal("5"), new BigDecimal("47680"), fecha, null);

        assertTrue(resultado);
    }

    @Test
    void esFullFalso_DevuelveFalseSinConsultarNingunRepositorio() {
        boolean resultado = fuelFullConsistencyService.fullInconsistente(
                5, null, false, new BigDecimal("5"), new BigDecimal("47680"), fecha, null);

        assertFalse(resultado);
        verifyNoInteractions(refuelingRecordsRepository, assetFuelConfigRepository);
    }

    @Test
    void esFullNulo_DevuelveFalseSinConsultarNingunRepositorio() {
        boolean resultado = fuelFullConsistencyService.fullInconsistente(
                5, null, null, new BigDecimal("5"), new BigDecimal("47680"), fecha, null);

        assertFalse(resultado);
        verifyNoInteractions(refuelingRecordsRepository, assetFuelConfigRepository);
    }

    @Test
    void sinTanqueoAnterior_DevuelveFalseSinLanzarError() {
        when(refuelingRecordsRepository.findAnteriorPorVehicleId(5, fecha)).thenReturn(Optional.empty());

        boolean resultado = fuelFullConsistencyService.fullInconsistente(
                5, null, true, new BigDecimal("5"), new BigDecimal("47680"), fecha, null);

        assertFalse(resultado);
        verifyNoInteractions(assetFuelConfigRepository);
    }

    @Test
    void sinConfiguracionDeConsumoEstandar_DevuelveFalse() {
        when(refuelingRecordsRepository.findAnteriorPorVehicleId(5, fecha)).thenReturn(Optional.of(anteriorVehiculo(new BigDecimal("47498"))));
        when(assetFuelConfigRepository.findByVehicleId(5)).thenReturn(Optional.empty());

        boolean resultado = fuelFullConsistencyService.fullInconsistente(
                5, null, true, new BigDecimal("5"), new BigDecimal("47680"), fecha, null);

        assertFalse(resultado);
    }

    @Test
    void consumoEstandarEnCeroOMenos_DevuelveFalseSinLanzarError() {
        when(refuelingRecordsRepository.findAnteriorPorVehicleId(5, fecha)).thenReturn(Optional.of(anteriorVehiculo(new BigDecimal("47498"))));
        when(assetFuelConfigRepository.findByVehicleId(5)).thenReturn(Optional.of(
                AssetFuelConfigEntity.builder().vehicleId(5).consumoEstandar(BigDecimal.ZERO).unidadConsumo("KM_POR_GALON").build()));

        boolean resultado = fuelFullConsistencyService.fullInconsistente(
                5, null, true, new BigDecimal("5"), new BigDecimal("47680"), fecha, null);

        assertFalse(resultado);
    }

    @Test
    void horometroRetrocedeRespectoAlAnterior_DevuelveFalse() {
        // Ya se marca aparte como alerta en Rendimiento (horometroRetrocedio) — acá
        // no tiene sentido proyectar un recorrido negativo.
        when(refuelingRecordsRepository.findAnteriorPorVehicleId(5, fecha)).thenReturn(Optional.of(anteriorVehiculo(new BigDecimal("47800"))));
        when(assetFuelConfigRepository.findByVehicleId(5)).thenReturn(Optional.of(
                AssetFuelConfigEntity.builder().vehicleId(5).consumoEstandar(new BigDecimal("11")).unidadConsumo("KM_POR_GALON").build()));

        boolean resultado = fuelFullConsistencyService.fullInconsistente(
                5, null, true, new BigDecimal("5"), new BigDecimal("47680"), fecha, null);

        assertFalse(resultado);
    }

    @Test
    void maquinaConUnidadPorHora_UsaFindAnteriorPorMachineId() {
        when(refuelingRecordsRepository.findAnteriorPorMachineId(10L, fecha)).thenReturn(Optional.of(
                RefuelingRecordsEntity.builder().id(1L).machineId(10L).horometroKm(new BigDecimal("100")).build()));
        when(assetFuelConfigRepository.findByMachineId(10L)).thenReturn(Optional.of(
                AssetFuelConfigEntity.builder().machineId(10L).consumoEstandar(new BigDecimal("0.5")).unidadConsumo("GAL_POR_HORA").build()));

        // 50 horas * 0.5 gal/hora = 25 gal proyectados; tanqueó 40 -> excede lo proyectado, no es "insuficiente".
        boolean resultado = fuelFullConsistencyService.fullInconsistente(
                null, 10L, true, new BigDecimal("40"), new BigDecimal("150"), fecha, null);

        assertFalse(resultado);
    }

    // ---- cantidadEsperadaParaLleno(): valor de referencia expuesto para mostrar
    // contra qué se comparó, mismo patrón que AssetFuelCapacityService/FuelPriceAnomalyService ----

    @Test
    void cantidadEsperadaParaLleno_CalculaLaProyeccionEstandar() {
        when(refuelingRecordsRepository.findAnteriorPorVehicleId(5, fecha)).thenReturn(Optional.of(anteriorVehiculo(new BigDecimal("47498"))));
        when(assetFuelConfigRepository.findByVehicleId(5)).thenReturn(Optional.of(
                AssetFuelConfigEntity.builder().vehicleId(5).consumoEstandar(new BigDecimal("11")).unidadConsumo("KM_POR_GALON").build()));

        BigDecimal esperado = fuelFullConsistencyService.cantidadEsperadaParaLleno(
                5, null, true, new BigDecimal("47680"), fecha, null);

        // 182 km / 11 km-por-gal = 16.545...
        assertEquals(0, new BigDecimal("16.545").compareTo(esperado));
    }

    @Test
    void cantidadEsperadaParaLleno_SeAcotaALaCapacidadConfiguradaSiLaSupera() {
        when(refuelingRecordsRepository.findAnteriorPorVehicleId(5, fecha)).thenReturn(Optional.of(anteriorVehiculo(new BigDecimal("47000"))));
        when(assetFuelConfigRepository.findByVehicleId(5)).thenReturn(Optional.of(
                AssetFuelConfigEntity.builder().vehicleId(5).consumoEstandar(new BigDecimal("11")).unidadConsumo("KM_POR_GALON").build()));

        // 680 km / 11 = 61.8 gal proyectados, pero el tanque solo admite 50 -> se acota a 50.
        BigDecimal esperado = fuelFullConsistencyService.cantidadEsperadaParaLleno(
                5, null, true, new BigDecimal("47680"), fecha, new BigDecimal("50"));

        assertEquals(0, new BigDecimal("50").compareTo(esperado));
    }

    @Test
    void cantidadEsperadaParaLleno_EsFullFalso_DevuelveNullSinConsultarNada() {
        assertNull(fuelFullConsistencyService.cantidadEsperadaParaLleno(
                5, null, false, new BigDecimal("47680"), fecha, null));

        verifyNoInteractions(refuelingRecordsRepository, assetFuelConfigRepository);
    }
}
