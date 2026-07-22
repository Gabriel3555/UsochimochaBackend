package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.application.dto.FuelPerformanceResponse;
import com.app.usochicamochabackend.fuel.infrastructure.entity.AssetFuelConfigEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.AssetFuelConfigRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuelPerformanceServiceTest {

    @Mock private RefuelingRecordsRepository refuelingRecordsRepository;
    @Mock private AssetFuelConfigRepository assetFuelConfigRepository;
    @Mock private VehicleRepository vehicleRepository;

    private FuelPerformanceService fuelPerformanceService;

    @BeforeEach
    void setUp() {
        fuelPerformanceService = new FuelPerformanceService(refuelingRecordsRepository, assetFuelConfigRepository, vehicleRepository);
        ReflectionTestUtils.setField(fuelPerformanceService, "tolerancia", new BigDecimal("0.15"));
    }

    @Test
    void maquinaConTanqueoPrevioYConfig_CalculaProyectadoYDiferencia() {
        RefuelingRecordsEntity actual = RefuelingRecordsEntity.builder()
                .id(2L).machineId(10L).horometroKm(new BigDecimal("150"))
                .cantidadGalones(new BigDecimal("40"))
                .fechaRegistro(Timestamp.valueOf(LocalDateTime.of(2026, 7, 15, 10, 0)))
                .build();
        RefuelingRecordsEntity anterior = RefuelingRecordsEntity.builder()
                .id(1L).machineId(10L).horometroKm(new BigDecimal("100")).build();

        when(refuelingRecordsRepository.findByMachineIdIsNotNullAndFechaRegistroBetween(any(), any()))
                .thenReturn(List.of(actual));
        when(refuelingRecordsRepository.findAnteriorPorMachineId(anyLong(), any()))
                .thenReturn(Optional.of(anterior));
        when(assetFuelConfigRepository.findByMachineId(10L)).thenReturn(Optional.of(
                AssetFuelConfigEntity.builder().machineId(10L).consumoEstandar(new BigDecimal("0.5")).unidadConsumo("GAL_POR_HORA").build()));

        List<FuelPerformanceResponse> resultado = fuelPerformanceService.obtenerRendimiento(
                "MAQUINARIA", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertEquals(1, resultado.size());
        FuelPerformanceResponse fila = resultado.get(0);
        // 50 horas ejecutadas * 0.5 gal/hora = 25 galones proyectados; real 40 -> diferencia 15
        assertEquals(0, new BigDecimal("50").compareTo(fila.ejecutado()));
        assertEquals(0, new BigDecimal("25").compareTo(fila.galonesProyectados()));
        assertEquals(0, new BigDecimal("15").compareTo(fila.diferencia()));
        assertTrue(fila.alerta());
    }

    @Test
    void maquinaSinTanqueoPrevio_QuedaExcluidaDelReporte() {
        RefuelingRecordsEntity actual = RefuelingRecordsEntity.builder()
                .id(2L).machineId(10L).horometroKm(new BigDecimal("150"))
                .cantidadGalones(new BigDecimal("40"))
                .fechaRegistro(Timestamp.valueOf(LocalDateTime.of(2026, 7, 15, 10, 0)))
                .build();
        when(refuelingRecordsRepository.findByMachineIdIsNotNullAndFechaRegistroBetween(any(), any()))
                .thenReturn(List.of(actual));
        when(refuelingRecordsRepository.findAnteriorPorMachineId(anyLong(), any()))
                .thenReturn(Optional.empty());

        List<FuelPerformanceResponse> resultado = fuelPerformanceService.obtenerRendimiento(
                "MAQUINARIA", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertTrue(resultado.isEmpty());
    }

    @Test
    void tipoInvalido_Lanza400() {
        org.springframework.web.server.ResponseStatusException ex = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> fuelPerformanceService.obtenerRendimiento("INVALIDO", null, null));

        assertEquals(400, ex.getStatusCode().value());
    }
}
