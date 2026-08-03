package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.infrastructure.entity.AssetFuelConfigEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.AssetFuelConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetFuelCapacityServiceTest {

    @Mock
    private AssetFuelConfigRepository assetFuelConfigRepository;

    private AssetFuelCapacityService assetFuelCapacityService;

    @BeforeEach
    void setUp() {
        assetFuelCapacityService = new AssetFuelCapacityService(assetFuelConfigRepository);
    }

    @Test
    void vehiculoConCantidadMayorALaCapacidadConfigurada_DevuelveTrue() {
        when(assetFuelConfigRepository.findByVehicleId(5)).thenReturn(Optional.of(
                AssetFuelConfigEntity.builder().vehicleId(5).tanqueCapacidadGal(new BigDecimal("10")).build()));

        boolean resultado = assetFuelCapacityService.excedeCapacidad(5, null, new BigDecimal("11"));

        assertTrue(resultado);
    }

    @Test
    void vehiculoConCantidadMenorOIgualALaCapacidad_DevuelveFalse() {
        when(assetFuelConfigRepository.findByVehicleId(5)).thenReturn(Optional.of(
                AssetFuelConfigEntity.builder().vehicleId(5).tanqueCapacidadGal(new BigDecimal("10")).build()));

        assertFalse(assetFuelCapacityService.excedeCapacidad(5, null, new BigDecimal("10")));
        assertFalse(assetFuelCapacityService.excedeCapacidad(5, null, new BigDecimal("9")));
    }

    @Test
    void maquinaConCantidadMayorALaCapacidadConfigurada_DevuelveTrue() {
        when(assetFuelConfigRepository.findByMachineId(10L)).thenReturn(Optional.of(
                AssetFuelConfigEntity.builder().machineId(10L).tanqueCapacidadGal(new BigDecimal("50")).build()));

        boolean resultado = assetFuelCapacityService.excedeCapacidad(null, 10L, new BigDecimal("60"));

        assertTrue(resultado);
    }

    @Test
    void sinConfiguracionParaElActivo_DevuelveFalseSinLanzarError() {
        when(assetFuelConfigRepository.findByVehicleId(5)).thenReturn(Optional.empty());

        assertFalse(assetFuelCapacityService.excedeCapacidad(5, null, new BigDecimal("999")));
    }

    @Test
    void configuracionSinCapacidadDefinida_DevuelveFalse() {
        when(assetFuelConfigRepository.findByVehicleId(5)).thenReturn(Optional.of(
                AssetFuelConfigEntity.builder().vehicleId(5).tanqueCapacidadGal(null).build()));

        assertFalse(assetFuelCapacityService.excedeCapacidad(5, null, new BigDecimal("999")));
    }

    @Test
    void cantidadGalonesNula_DevuelveFalseSinConsultarRepositorio() {
        assertFalse(assetFuelCapacityService.excedeCapacidad(5, null, null));

        verifyNoInteractions(assetFuelConfigRepository);
    }
}
