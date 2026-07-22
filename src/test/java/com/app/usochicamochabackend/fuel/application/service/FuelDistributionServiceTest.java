package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.application.dto.FuelDistributionResponse;
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
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuelDistributionServiceTest {

    @Mock private RefuelingRecordsRepository refuelingRecordsRepository;
    @Mock private FuelReintegrationsRepository fuelReintegrationsRepository;

    @InjectMocks
    private FuelDistributionService fuelDistributionService;

    @Test
    void tanqueoBomba_ValorizaYTanqueoAlmacen_NoValoriza() {
        RefuelingRecordsEntity bomba = RefuelingRecordsEntity.builder()
                .id(1L).lugar("BOMBA").areaCosto("DISTRITO").fuelTypeId(1L)
                .cantidadGalones(new BigDecimal("20")).precioUnitario(new BigDecimal("10000"))
                .fechaRegistro(Timestamp.valueOf(LocalDateTime.of(2026, 7, 10, 8, 0)))
                .build();
        RefuelingRecordsEntity almacen = RefuelingRecordsEntity.builder()
                .id(2L).lugar("ALMACEN").areaCosto("DISTRITO").fuelTypeId(1L)
                .cantidadGalones(new BigDecimal("30"))
                .fechaRegistro(Timestamp.valueOf(LocalDateTime.of(2026, 7, 11, 8, 0)))
                .build();
        when(refuelingRecordsRepository.findByAreaCostoAndFechaRegistroBetween(
                org.mockito.ArgumentMatchers.eq("DISTRITO"), any(), any()))
                .thenReturn(List.of(bomba, almacen));
        when(fuelReintegrationsRepository.findByRefuelingId(anyLong())).thenReturn(List.of());

        FuelDistributionResponse response = fuelDistributionService.obtenerDistribucion(
                "DISTRITO", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertEquals(0, new BigDecimal("50").compareTo(response.totalGalonesDespachados()));
        assertEquals(0, new BigDecimal("200000").compareTo(response.totalCostoDespachado()));

        FuelDistributionResponse.Fila filaBomba = response.filas().stream().filter(f -> f.refuelingId() == 1L).findFirst().orElseThrow();
        FuelDistributionResponse.Fila filaAlmacen = response.filas().stream().filter(f -> f.refuelingId() == 2L).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("200000").compareTo(filaBomba.valorDespachado()));
        assertNull(filaAlmacen.valorDespachado());
    }

    @Test
    void areaInvalida_Lanza400() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> fuelDistributionService.obtenerDistribucion("INVALIDA", null, null));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void tanqueoConReintegro_TraeCantidadYValorReintegrados() {
        RefuelingRecordsEntity almacen = RefuelingRecordsEntity.builder()
                .id(3L).lugar("ALMACEN").areaCosto("DISTRITO").fuelTypeId(1L)
                .cantidadGalones(new BigDecimal("30"))
                .fechaRegistro(Timestamp.valueOf(LocalDateTime.of(2026, 7, 12, 8, 0)))
                .build();
        FuelReintegrationsEntity reintegro = FuelReintegrationsEntity.builder()
                .id(1L).refuelingId(3L).cantidadReintegrada(new BigDecimal("5")).valorReintegro(null).build();

        when(refuelingRecordsRepository.findByAreaCostoAndFechaRegistroBetween(
                org.mockito.ArgumentMatchers.eq("DISTRITO"), any(), any()))
                .thenReturn(List.of(almacen));
        when(fuelReintegrationsRepository.findByRefuelingId(3L)).thenReturn(List.of(reintegro));

        FuelDistributionResponse response = fuelDistributionService.obtenerDistribucion(
                "DISTRITO", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        FuelDistributionResponse.Fila fila = response.filas().get(0);
        assertEquals(0, new BigDecimal("5").compareTo(fila.cantidadReintegrada()));
    }
}
