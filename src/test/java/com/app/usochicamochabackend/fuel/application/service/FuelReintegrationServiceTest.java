package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.fuel.application.dto.FuelReintegrationRequest;
import com.app.usochicamochabackend.fuel.application.dto.FuelReintegrationResponse;
import com.app.usochicamochabackend.fuel.application.port.AdjustFuelInventoryUseCase;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelReintegrationsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelReintegrationsRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
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
class FuelReintegrationServiceTest {

    @Mock private FuelReintegrationsRepository fuelReintegrationsRepository;
    @Mock private RefuelingRecordsRepository refuelingRecordsRepository;
    @Mock private AdjustFuelInventoryUseCase adjustFuelInventoryUseCase;
    @Mock private SaveActionUseCase saveActionUseCase;

    @InjectMocks
    private FuelReintegrationService fuelReintegrationService;

    @Test
    void reintegroDeTanqueoAlmacen_DevuelveInventarioYNoValoriza() {
        RefuelingRecordsEntity tanqueo = RefuelingRecordsEntity.builder()
                .id(1L).lugar("ALMACEN").areaCosto("DISTRITO").fuelTypeId(1L)
                .cantidadGalones(new BigDecimal("30")).build();
        when(refuelingRecordsRepository.findById(1L)).thenReturn(Optional.of(tanqueo));
        when(fuelReintegrationsRepository.save(any())).thenAnswer(invocation -> {
            FuelReintegrationsEntity e = invocation.getArgument(0);
            e.setId(1L);
            return e;
        });

        FuelReintegrationRequest request = new FuelReintegrationRequest(1L, new BigDecimal("5"));
        FuelReintegrationResponse response = fuelReintegrationService.registrar(request, 7L);

        assertNull(response.valorReintegro());
        verify(adjustFuelInventoryUseCase).increment("DISTRITO", 1L, new BigDecimal("5"));
        verify(saveActionUseCase).save(anyString());
    }

    @Test
    void reintegroDeTanqueoBomba_CalculaValorYNoTocaInventario() {
        RefuelingRecordsEntity tanqueo = RefuelingRecordsEntity.builder()
                .id(2L).lugar("BOMBA").areaCosto("DISTRITO").fuelTypeId(1L)
                .cantidadGalones(new BigDecimal("30")).precioUnitario(new BigDecimal("10000")).build();
        when(refuelingRecordsRepository.findById(2L)).thenReturn(Optional.of(tanqueo));
        when(fuelReintegrationsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FuelReintegrationRequest request = new FuelReintegrationRequest(2L, new BigDecimal("5"));
        FuelReintegrationResponse response = fuelReintegrationService.registrar(request, 7L);

        assertEquals(0, new BigDecimal("50000").compareTo(response.valorReintegro()));
        verifyNoInteractions(adjustFuelInventoryUseCase);
    }

    @Test
    void reintegroQueSuperaLaCantidadOriginal_Lanza400() {
        RefuelingRecordsEntity tanqueo = RefuelingRecordsEntity.builder()
                .id(1L).lugar("ALMACEN").areaCosto("DISTRITO").fuelTypeId(1L)
                .cantidadGalones(new BigDecimal("30")).build();
        when(refuelingRecordsRepository.findById(1L)).thenReturn(Optional.of(tanqueo));

        FuelReintegrationRequest request = new FuelReintegrationRequest(1L, new BigDecimal("31"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> fuelReintegrationService.registrar(request, 7L));

        assertEquals(400, ex.getStatusCode().value());
        verify(fuelReintegrationsRepository, never()).save(any());
    }

    @Test
    void reintegroDeTanqueoInexistente_LanzaResourceNotFound() {
        when(refuelingRecordsRepository.findById(99L)).thenReturn(Optional.empty());

        FuelReintegrationRequest request = new FuelReintegrationRequest(99L, new BigDecimal("5"));

        assertThrows(ResourceNotFoundException.class, () -> fuelReintegrationService.registrar(request, 7L));
    }
}
