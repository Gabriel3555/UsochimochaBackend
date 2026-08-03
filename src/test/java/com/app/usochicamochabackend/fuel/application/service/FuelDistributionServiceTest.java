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
                .esFull(true).discrepanciaValor(false)
                .fechaRegistro(Timestamp.valueOf(LocalDateTime.of(2026, 7, 10, 8, 0)))
                .build();
        RefuelingRecordsEntity almacen = RefuelingRecordsEntity.builder()
                .id(2L).lugar("ALMACEN").areaCosto("DISTRITO").fuelTypeId(1L)
                .cantidadGalones(new BigDecimal("30")).esFull(false)
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

        // Full/Precio/Discr./Área/Lugar ahora viajan por fila (necesarios para la
        // tabla agrupada por origen de la pestaña fusionada Tanqueo y Distribución).
        assertEquals("DISTRITO", filaBomba.areaCosto());
        assertTrue(filaBomba.esFull());
        assertEquals(0, new BigDecimal("10000").compareTo(filaBomba.precioUnitario()));
        assertFalse(filaBomba.discrepanciaValor());
        assertEquals("BOMBA", filaBomba.lugar());
        assertEquals("ALMACEN", filaAlmacen.lugar());
    }

    @Test
    void areaNulaOTodas_CombinaDistritoYAsociacion() {
        RefuelingRecordsEntity distrito = RefuelingRecordsEntity.builder()
                .id(4L).lugar("BOMBA").areaCosto("DISTRITO").fuelTypeId(1L)
                .cantidadGalones(new BigDecimal("10")).precioUnitario(new BigDecimal("10000"))
                .fechaRegistro(Timestamp.valueOf(LocalDateTime.of(2026, 7, 10, 8, 0)))
                .build();
        RefuelingRecordsEntity asociacion = RefuelingRecordsEntity.builder()
                .id(5L).lugar("BOMBA").areaCosto("ASOCIACION").fuelTypeId(1L)
                .cantidadGalones(new BigDecimal("15")).precioUnitario(new BigDecimal("10000"))
                .fechaRegistro(Timestamp.valueOf(LocalDateTime.of(2026, 7, 11, 8, 0)))
                .build();
        when(refuelingRecordsRepository.findByFechaRegistroBetween(any(), any()))
                .thenReturn(List.of(distrito, asociacion));
        when(fuelReintegrationsRepository.findByRefuelingId(anyLong())).thenReturn(List.of());

        FuelDistributionResponse response = fuelDistributionService.obtenerDistribucion(
                null, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertEquals(2, response.filas().size());
        assertEquals(0, new BigDecimal("25").compareTo(response.totalGalonesDespachados()));
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
