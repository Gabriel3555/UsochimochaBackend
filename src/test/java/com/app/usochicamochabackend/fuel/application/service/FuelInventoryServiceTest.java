package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelInventoryEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FuelInventoryServiceTest {

    private static final String AREA_COSTO = "DISTRITO";
    private static final Long FUEL_TYPE_ID = 1L;

    @Mock
    private FuelInventoryRepository fuelInventoryRepository;

    @InjectMocks
    private FuelInventoryService fuelInventoryService;

    private FuelInventoryEntity existingRow;

    @BeforeEach
    void setUp() {
        existingRow = FuelInventoryEntity.builder()
                .id(1L)
                .areaCosto(AREA_COSTO)
                .fuelTypeId(FUEL_TYPE_ID)
                .cantidadDisponible(new BigDecimal("100.000"))
                .build();
    }

    @Test
    void incrementSumaALaCantidadExistenteYGuarda() {
        when(fuelInventoryRepository.findByAreaCostoAndFuelTypeId(AREA_COSTO, FUEL_TYPE_ID))
                .thenReturn(Optional.of(existingRow));

        fuelInventoryService.increment(AREA_COSTO, FUEL_TYPE_ID, new BigDecimal("50.000"));

        ArgumentCaptor<FuelInventoryEntity> captor = ArgumentCaptor.forClass(FuelInventoryEntity.class);
        verify(fuelInventoryRepository).save(captor.capture());
        assertEquals(0, new BigDecimal("150.000").compareTo(captor.getValue().getCantidadDisponible()));
    }

    @Test
    void incrementLanzaResourceNotFoundSiNoExisteFilaDeInventario() {
        when(fuelInventoryRepository.findByAreaCostoAndFuelTypeId(AREA_COSTO, FUEL_TYPE_ID))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> fuelInventoryService.increment(AREA_COSTO, FUEL_TYPE_ID, BigDecimal.TEN));

        verify(fuelInventoryRepository, never()).save(any());
    }

    @Test
    void decrementRestaDeLaCantidadExistenteYGuarda() {
        when(fuelInventoryRepository.findByAreaCostoAndFuelTypeId(AREA_COSTO, FUEL_TYPE_ID))
                .thenReturn(Optional.of(existingRow));

        fuelInventoryService.decrement(AREA_COSTO, FUEL_TYPE_ID, new BigDecimal("30.000"));

        ArgumentCaptor<FuelInventoryEntity> captor = ArgumentCaptor.forClass(FuelInventoryEntity.class);
        verify(fuelInventoryRepository).save(captor.capture());
        assertEquals(0, new BigDecimal("70.000").compareTo(captor.getValue().getCantidadDisponible()));
    }

    @Test
    void decrementLanzaConflictSiElSaldoQuedaNegativo() {
        when(fuelInventoryRepository.findByAreaCostoAndFuelTypeId(AREA_COSTO, FUEL_TYPE_ID))
                .thenReturn(Optional.of(existingRow));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> fuelInventoryService.decrement(AREA_COSTO, FUEL_TYPE_ID, new BigDecimal("150.000")));

        assertEquals(409, ex.getStatusCode().value());
        verify(fuelInventoryRepository, never()).save(any());
    }

    @Test
    void decrementLanzaResourceNotFoundSiNoExisteFilaDeInventario() {
        when(fuelInventoryRepository.findByAreaCostoAndFuelTypeId(AREA_COSTO, FUEL_TYPE_ID))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> fuelInventoryService.decrement(AREA_COSTO, FUEL_TYPE_ID, BigDecimal.TEN));

        verify(fuelInventoryRepository, never()).save(any());
    }
}
