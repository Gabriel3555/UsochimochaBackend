package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.application.dto.FuelPerformanceResponse;
import com.app.usochicamochabackend.fuel.infrastructure.entity.AssetFuelConfigEntity;
import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.AssetFuelConfigRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import com.app.usochicamochabackend.machine.infrastructure.entity.MachineEntity;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.MarcaModeloEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import com.app.usochicamochabackend.catalog.infrastructure.entity.TipoVehiculoEntity;
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
    @Mock private MachineRepository machineRepository;

    private FuelPerformanceService fuelPerformanceService;

    @BeforeEach
    void setUp() {
        fuelPerformanceService = new FuelPerformanceService(refuelingRecordsRepository, assetFuelConfigRepository, vehicleRepository, machineRepository);
        ReflectionTestUtils.setField(fuelPerformanceService, "tolerancia", new BigDecimal("0.15"));
    }

    @Test
    void maquinaConTanqueoPrevioYConfig_CalculaProyectadoYDiferencia() {
        RefuelingRecordsEntity actual = RefuelingRecordsEntity.builder()
                .id(2L).machineId(10L).horometroKm(new BigDecimal("150"))
                .cantidadGalones(new BigDecimal("40")).esFull(true).fuelTypeId(3L)
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
        when(machineRepository.findById(10L)).thenReturn(Optional.of(
                MachineEntity.builder().id(10L).name("Excavadora CAT 01").build()));

        List<FuelPerformanceResponse> resultado = fuelPerformanceService.obtenerRendimiento(
                "MAQUINARIA", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertEquals(1, resultado.size());
        FuelPerformanceResponse fila = resultado.get(0);
        // 50 horas ejecutadas * 0.5 gal/hora = 25 galones proyectados; real 40 -> diferencia 15
        assertEquals(0, new BigDecimal("50").compareTo(fila.ejecutado()));
        assertEquals(0, new BigDecimal("25").compareTo(fila.galonesProyectados()));
        assertEquals(0, new BigDecimal("15").compareTo(fila.diferencia()));
        assertTrue(fila.alerta());
        // Máquinas no tienen placa — se identifican por su nombre, igual que en el
        // módulo de Maquinaria (no hay categoría formal).
        assertEquals("Excavadora CAT 01", fila.identificacionActivo());
        assertTrue(fila.esFull());
        // El producto/combustible de la fila debe salir del tanqueo real, no de la
        // config default del activo.
        assertEquals(3L, fila.fuelTypeId());
    }

    @Test
    void vehiculoConTanqueoPrevioYConfig_IdentificacionActivoUsaPlacaYMarca() {
        // El "tipo de vehículo" (Camioneta, Sedán...) no identifica CUÁL vehículo hizo
        // el tanqueo si hay varios del mismo tipo — se necesita la placa (única por
        // vehículo) para poder ubicarlo rápido en Rendimiento, igual que ya se hace
        // en Tanqueo y Distribución.
        RefuelingRecordsEntity actual = RefuelingRecordsEntity.builder()
                .id(4L).vehicleId(5).horometroKm(new BigDecimal("47680"))
                .cantidadGalones(new BigDecimal("17")).esFull(false).fuelTypeId(2L)
                .fechaRegistro(Timestamp.valueOf(LocalDateTime.of(2026, 7, 21, 10, 0)))
                .build();
        RefuelingRecordsEntity anterior = RefuelingRecordsEntity.builder()
                .id(3L).vehicleId(5).horometroKm(new BigDecimal("47498")).build();

        when(refuelingRecordsRepository.findByVehicleIdIsNotNullAndFechaRegistroBetween(any(), any()))
                .thenReturn(List.of(actual));
        when(refuelingRecordsRepository.findAnteriorPorVehicleId(any(), any()))
                .thenReturn(Optional.of(anterior));
        when(assetFuelConfigRepository.findByVehicleId(5)).thenReturn(Optional.of(
                AssetFuelConfigEntity.builder().vehicleId(5).consumoEstandar(new BigDecimal("11")).unidadConsumo("KM_POR_GALON").build()));
        TipoVehiculoEntity camioneta = TipoVehiculoEntity.builder().nombreTipo("Camioneta").build();
        MarcaModeloEntity toyota = MarcaModeloEntity.builder().descripcion("Toyota").build();
        when(vehicleRepository.findById(5)).thenReturn(Optional.of(
                VehicleEntity.builder().idVehiculo(5).placa("ABC123").marca(toyota).tipoVehiculo(camioneta).build()));

        List<FuelPerformanceResponse> resultado = fuelPerformanceService.obtenerRendimiento(
                "VEHICULO", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertEquals(1, resultado.size());
        FuelPerformanceResponse fila = resultado.get(0);
        assertEquals("ABC123 — Toyota", fila.identificacionActivo());
        assertFalse(fila.esFull());
        assertEquals(2L, fila.fuelTypeId());
    }

    @Test
    void maquinaAGasConTanqueoPrevioYConfig_CalculaProyectadoConUnidadM3PorHora() {
        RefuelingRecordsEntity actual = RefuelingRecordsEntity.builder()
                .id(2L).machineId(11L).horometroKm(new BigDecimal("150"))
                .cantidadGalones(new BigDecimal("35"))
                .fechaRegistro(Timestamp.valueOf(LocalDateTime.of(2026, 7, 15, 10, 0)))
                .build();
        RefuelingRecordsEntity anterior = RefuelingRecordsEntity.builder()
                .id(1L).machineId(11L).horometroKm(new BigDecimal("100")).build();

        when(refuelingRecordsRepository.findByMachineIdIsNotNullAndFechaRegistroBetween(any(), any()))
                .thenReturn(List.of(actual));
        when(refuelingRecordsRepository.findAnteriorPorMachineId(anyLong(), any()))
                .thenReturn(Optional.of(anterior));
        when(assetFuelConfigRepository.findByMachineId(11L)).thenReturn(Optional.of(
                AssetFuelConfigEntity.builder().machineId(11L).consumoEstandar(new BigDecimal("0.6")).unidadConsumo("M3_POR_HORA").build()));

        List<FuelPerformanceResponse> resultado = fuelPerformanceService.obtenerRendimiento(
                "MAQUINARIA", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertEquals(1, resultado.size());
        FuelPerformanceResponse fila = resultado.get(0);
        // 50 horas ejecutadas * 0.6 m3/hora = 30 proyectados; real 35 -> diferencia 5
        assertEquals(0, new BigDecimal("50").compareTo(fila.ejecutado()));
        assertEquals(0, new BigDecimal("30").compareTo(fila.galonesProyectados()));
        assertEquals(0, new BigDecimal("5").compareTo(fila.diferencia()));
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
    void horometroRetrocedeRespectoAlAnterior_MarcaAlertaSiempre() {
        // El horómetro/km actual es MENOR que el anterior (150 -> 90): un dato mal
        // digitado o corregido hacia atrás. El cálculo normal (ejecutado negativo)
        // no tiene sentido, así que debe marcarse como alerta sin importar qué tan
        // "parecida" salga la diferencia con el proyectado.
        RefuelingRecordsEntity actual = RefuelingRecordsEntity.builder()
                .id(2L).machineId(10L).horometroKm(new BigDecimal("90"))
                .cantidadGalones(new BigDecimal("40"))
                .fechaRegistro(Timestamp.valueOf(LocalDateTime.of(2026, 7, 15, 10, 0)))
                .build();
        RefuelingRecordsEntity anterior = RefuelingRecordsEntity.builder()
                .id(1L).machineId(10L).horometroKm(new BigDecimal("150")).build();

        when(refuelingRecordsRepository.findByMachineIdIsNotNullAndFechaRegistroBetween(any(), any()))
                .thenReturn(List.of(actual));
        when(refuelingRecordsRepository.findAnteriorPorMachineId(anyLong(), any()))
                .thenReturn(Optional.of(anterior));
        when(assetFuelConfigRepository.findByMachineId(10L)).thenReturn(Optional.of(
                AssetFuelConfigEntity.builder().machineId(10L).consumoEstandar(new BigDecimal("0.5")).unidadConsumo("GAL_POR_HORA").build()));

        List<FuelPerformanceResponse> resultado = fuelPerformanceService.obtenerRendimiento(
                "MAQUINARIA", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertEquals(1, resultado.size());
        assertEquals(0, new BigDecimal("-60").compareTo(resultado.get(0).ejecutado()));
        assertTrue(resultado.get(0).alerta(), "Un horómetro/km que retrocede siempre debe marcarse como alerta");
    }

    @Test
    void consumoEstandarConfiguradoEnCeroOMenos_QuedaExcluidaDelReporte_NoRompeElCalculo() {
        // Config vieja/inválida (consumoEstandar<=0) no debería tumbar todo el
        // reporte con una división por cero — se excluye esa fila, igual que
        // cuando falta config por completo.
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
                AssetFuelConfigEntity.builder().machineId(10L).consumoEstandar(BigDecimal.ZERO).unidadConsumo("KM_POR_GALON").build()));

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
