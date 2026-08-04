package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.catalog.infrastructure.entity.TipoVehiculoEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.AssetFuelConfigEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.AssetFuelConfigRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Mock
    private VehicleRepository vehicleRepository;

    private AssetFuelCapacityService assetFuelCapacityService;

    @BeforeEach
    void setUp() {
        assetFuelCapacityService = new AssetFuelCapacityService(assetFuelConfigRepository, vehicleRepository);
        ReflectionTestUtils.setField(assetFuelCapacityService, "maximoRazonableMoto", new BigDecimal("15"));
        ReflectionTestUtils.setField(assetFuelCapacityService, "maximoRazonableVehiculo", new BigDecimal("60"));
        ReflectionTestUtils.setField(assetFuelCapacityService, "maximoRazonableMaquinaria", new BigDecimal("500"));
    }

    private VehicleEntity vehiculo(String tipoNombre) {
        return VehicleEntity.builder().tipoVehiculo(TipoVehiculoEntity.builder().nombreTipo(tipoNombre).build()).build();
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

    @Test
    void motoConCantidadMuyPorEncimaDelMaximoRazonable_DevuelveTrue() {
        when(vehicleRepository.findById(6)).thenReturn(Optional.of(vehiculo("Motocicleta")));

        assertTrue(assetFuelCapacityService.cantidadFueraDeRangoTipico(6, null, new BigDecimal("300")));
    }

    @Test
    void motoConCantidadDentroDeLoRazonable_DevuelveFalse() {
        when(vehicleRepository.findById(6)).thenReturn(Optional.of(vehiculo("Motocicleta")));

        assertFalse(assetFuelCapacityService.cantidadFueraDeRangoTipico(6, null, new BigDecimal("5")));
    }

    @Test
    void vehiculoNoMoto_UsaElMaximoDeVehiculoNoElDeMoto() {
        when(vehicleRepository.findById(5)).thenReturn(Optional.of(vehiculo("Camioneta")));

        // 20 gal es razonable para un vehículo (tope 60) pero superaría el de moto (tope 15).
        assertFalse(assetFuelCapacityService.cantidadFueraDeRangoTipico(5, null, new BigDecimal("20")));
        assertTrue(assetFuelCapacityService.cantidadFueraDeRangoTipico(5, null, new BigDecimal("61")));
    }

    @Test
    void maquinaConCantidadMuyAlta_UsaElMaximoDeMaquinaria() {
        assertFalse(assetFuelCapacityService.cantidadFueraDeRangoTipico(null, 10L, new BigDecimal("300")));
        assertTrue(assetFuelCapacityService.cantidadFueraDeRangoTipico(null, 10L, new BigDecimal("501")));

        verifyNoInteractions(vehicleRepository);
    }

    @Test
    void cantidadFueraDeRangoTipico_CantidadNula_DevuelveFalse() {
        assertFalse(assetFuelCapacityService.cantidadFueraDeRangoTipico(5, null, null));

        verifyNoInteractions(vehicleRepository);
    }
}
