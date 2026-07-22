package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.fuel.application.dto.AssetFuelConfigRequest;
import com.app.usochicamochabackend.fuel.application.dto.AssetFuelConfigResponse;
import com.app.usochicamochabackend.fuel.infrastructure.entity.AssetFuelConfigEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.AssetFuelConfigRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelTypesRepository;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetFuelConfigServiceTest {

    @Mock private AssetFuelConfigRepository assetFuelConfigRepository;
    @Mock private FuelTypesRepository fuelTypesRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private MachineRepository machineRepository;
    @Mock private SaveActionUseCase saveActionUseCase;

    @InjectMocks
    private AssetFuelConfigService assetFuelConfigService;

    @Test
    void configurarVehiculo_CreaConfigSiNoExiste() {
        when(vehicleRepository.existsById(5)).thenReturn(true);
        when(fuelTypesRepository.existsById(1L)).thenReturn(true);
        when(assetFuelConfigRepository.findByVehicleId(5)).thenReturn(Optional.empty());
        when(assetFuelConfigRepository.save(any())).thenAnswer(invocation -> {
            AssetFuelConfigEntity e = invocation.getArgument(0);
            e.setId(1L);
            return e;
        });

        AssetFuelConfigRequest request = new AssetFuelConfigRequest(1L, new BigDecimal("30"), "KM_POR_GALON", null);
        AssetFuelConfigResponse response = assetFuelConfigService.configurarVehiculo(5, request);

        assertEquals(5, response.vehicleId());
        assertEquals("KM_POR_GALON", response.unidadConsumo());
        verify(saveActionUseCase).save(anyString());
    }

    @Test
    void configurarVehiculo_ActualizaSiYaExiste() {
        AssetFuelConfigEntity existente = AssetFuelConfigEntity.builder().id(9L).vehicleId(5).build();
        when(vehicleRepository.existsById(5)).thenReturn(true);
        when(fuelTypesRepository.existsById(1L)).thenReturn(true);
        when(assetFuelConfigRepository.findByVehicleId(5)).thenReturn(Optional.of(existente));
        when(assetFuelConfigRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AssetFuelConfigRequest request = new AssetFuelConfigRequest(1L, new BigDecimal("35"), "KM_POR_GALON", null);
        AssetFuelConfigResponse response = assetFuelConfigService.configurarVehiculo(5, request);

        assertEquals(9L, response.id());
        assertEquals(0, new BigDecimal("35").compareTo(response.consumoEstandar()));
    }

    @Test
    void configurarVehiculo_ConUnidadIncorrecta_Lanza400() {
        AssetFuelConfigRequest request = new AssetFuelConfigRequest(1L, new BigDecimal("30"), "GAL_POR_HORA", null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> assetFuelConfigService.configurarVehiculo(5, request));

        assertEquals(400, ex.getStatusCode().value());
        verify(assetFuelConfigRepository, never()).save(any());
    }

    @Test
    void configurarMaquina_ConUnidadIncorrecta_Lanza400() {
        AssetFuelConfigRequest request = new AssetFuelConfigRequest(1L, new BigDecimal("5"), "KM_POR_GALON", null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> assetFuelConfigService.configurarMaquina(10L, request));

        assertEquals(400, ex.getStatusCode().value());
        verify(assetFuelConfigRepository, never()).save(any());
    }
}
