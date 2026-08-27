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
import java.util.Map;
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
        ReflectionTestUtils.setField(fuelPerformanceService, "toleranciaFija", new BigDecimal("0.15"));
        ReflectionTestUtils.setField(fuelPerformanceService, "multiplicadorAprendido", 1.5);
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
        when(refuelingRecordsRepository.findByMachineIdOrderByFechaRegistroAsc(10L))
                .thenReturn(List.of(anterior, actual));
        when(assetFuelConfigRepository.findByMachineId(10L)).thenReturn(Optional.of(
                AssetFuelConfigEntity.builder().machineId(10L).consumoEstandar(new BigDecimal("2")).unidadConsumo("HORA_POR_GALON").build()));
        when(machineRepository.findById(10L)).thenReturn(Optional.of(
                MachineEntity.builder().id(10L).name("Excavadora CAT 01").build()));

        List<FuelPerformanceResponse> resultado = fuelPerformanceService.obtenerRendimiento(
                "MAQUINARIA", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertEquals(1, resultado.size());
        FuelPerformanceResponse fila = resultado.get(0);
        // 50 horas ejecutadas / 2 horas-por-galón = 25 galones proyectados; real 40 -> diferencia 15
        assertEquals(0, new BigDecimal("50").compareTo(fila.ejecutado()));
        assertEquals(0, new BigDecimal("25").compareTo(fila.galonesProyectados()));
        assertEquals(0, new BigDecimal("15").compareTo(fila.diferencia()));
        assertTrue(fila.alerta());
        // Sin historial previo (0 desviaciones), usa la tolerancia fija, no la aprendida.
        assertFalse(fila.usaRangoAprendido());
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
        when(refuelingRecordsRepository.findByVehicleIdOrderByFechaRegistroAsc(5))
                .thenReturn(List.of(anterior, actual));
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
    void maquinaAGasConTanqueoPrevioYConfig_CalculaProyectadoConUnidadHoraPorM3() {
        RefuelingRecordsEntity actual = RefuelingRecordsEntity.builder()
                .id(2L).machineId(11L).horometroKm(new BigDecimal("150"))
                .cantidadGalones(new BigDecimal("30"))
                .fechaRegistro(Timestamp.valueOf(LocalDateTime.of(2026, 7, 15, 10, 0)))
                .build();
        RefuelingRecordsEntity anterior = RefuelingRecordsEntity.builder()
                .id(1L).machineId(11L).horometroKm(new BigDecimal("100")).build();

        when(refuelingRecordsRepository.findByMachineIdIsNotNullAndFechaRegistroBetween(any(), any()))
                .thenReturn(List.of(actual));
        when(refuelingRecordsRepository.findByMachineIdOrderByFechaRegistroAsc(11L))
                .thenReturn(List.of(anterior, actual));
        when(assetFuelConfigRepository.findByMachineId(11L)).thenReturn(Optional.of(
                AssetFuelConfigEntity.builder().machineId(11L).consumoEstandar(new BigDecimal("2")).unidadConsumo("HORA_POR_M3").build()));

        List<FuelPerformanceResponse> resultado = fuelPerformanceService.obtenerRendimiento(
                "MAQUINARIA", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertEquals(1, resultado.size());
        FuelPerformanceResponse fila = resultado.get(0);
        // 50 horas ejecutadas / 2 horas-por-m3 = 25 proyectados; real 30 -> diferencia 5
        assertEquals(0, new BigDecimal("50").compareTo(fila.ejecutado()));
        assertEquals(0, new BigDecimal("25").compareTo(fila.galonesProyectados()));
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
        // Historial completo del activo = solo este tanqueo -> no hay línea base.
        when(refuelingRecordsRepository.findByMachineIdOrderByFechaRegistroAsc(10L))
                .thenReturn(List.of(actual));
        when(assetFuelConfigRepository.findByMachineId(10L)).thenReturn(Optional.of(
                AssetFuelConfigEntity.builder().machineId(10L).consumoEstandar(new BigDecimal("0.5")).unidadConsumo("HORA_POR_GALON").build()));

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
        when(refuelingRecordsRepository.findByMachineIdOrderByFechaRegistroAsc(10L))
                .thenReturn(List.of(anterior, actual));
        when(assetFuelConfigRepository.findByMachineId(10L)).thenReturn(Optional.of(
                AssetFuelConfigEntity.builder().machineId(10L).consumoEstandar(new BigDecimal("0.5")).unidadConsumo("HORA_POR_GALON").build()));

        List<FuelPerformanceResponse> resultado = fuelPerformanceService.obtenerRendimiento(
                "MAQUINARIA", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        assertEquals(1, resultado.size());
        assertEquals(0, new BigDecimal("-60").compareTo(resultado.get(0).ejecutado()));
        assertTrue(resultado.get(0).alerta(), "Un horómetro/km que retrocede siempre debe marcarse como alerta");
        assertFalse(resultado.get(0).usaRangoAprendido());
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

        when(refuelingRecordsRepository.findByMachineIdIsNotNullAndFechaRegistroBetween(any(), any()))
                .thenReturn(List.of(actual));
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

    // --- Tolerancia aprendida por activo (promedio ± desviación estándar) ---

    private RefuelingRecordsEntity tanqueoMaquina(long id, long machineId, String horometro, String galones, LocalDateTime fecha) {
        return RefuelingRecordsEntity.builder()
                .id(id).machineId(machineId).horometroKm(new BigDecimal(horometro))
                .cantidadGalones(new BigDecimal(galones)).fechaRegistro(Timestamp.valueOf(fecha))
                .build();
    }

    @Test
    void conDosOMasDesviacionesPreviasDelMismoActivo_UsaElRangoAprendidoEnVezDelFijo() {
        // Máquina 20, consumoEstandar=1 hora/galón (HORA_POR_GALON) para simplificar la
        // aritmética a mano: proyectado = ejecutado / 1 = ejecutado.
        // Secuencia (10 horas ejecutadas entre cada tanqueo, mismo ejecutado siempre):
        //   T1 (línea base, sin fila)              horómetro=0
        //   T2: real=11 -> diferencia=1  -> desviación=0.1   (0 previas -> fijo)
        //   T3: real=12 -> diferencia=2  -> desviación=0.2   (1 previa  -> fijo)
        //   T4: real=13 -> diferencia=3  -> desviación=0.3   (2 previas -> APRENDIDO)
        //   T5: real=11.6 -> diferencia=1.6 -> desviación=0.16 (3 previas -> APRENDIDO)
        RefuelingRecordsEntity t1 = tanqueoMaquina(1, 20, "0", "0", LocalDateTime.of(2026, 1, 1, 8, 0));
        RefuelingRecordsEntity t2 = tanqueoMaquina(2, 20, "10", "11", LocalDateTime.of(2026, 1, 2, 8, 0));
        RefuelingRecordsEntity t3 = tanqueoMaquina(3, 20, "20", "12", LocalDateTime.of(2026, 1, 3, 8, 0));
        RefuelingRecordsEntity t4 = tanqueoMaquina(4, 20, "30", "13", LocalDateTime.of(2026, 1, 4, 8, 0));
        RefuelingRecordsEntity t5 = tanqueoMaquina(5, 20, "40", "11.6", LocalDateTime.of(2026, 1, 5, 8, 0));

        when(refuelingRecordsRepository.findByMachineIdIsNotNullAndFechaRegistroBetween(any(), any()))
                .thenReturn(List.of(t2, t3, t4, t5));
        when(refuelingRecordsRepository.findByMachineIdOrderByFechaRegistroAsc(20L))
                .thenReturn(List.of(t1, t2, t3, t4, t5));
        when(assetFuelConfigRepository.findByMachineId(20L)).thenReturn(Optional.of(
                AssetFuelConfigEntity.builder().machineId(20L).consumoEstandar(new BigDecimal("1")).unidadConsumo("HORA_POR_GALON").build()));

        List<FuelPerformanceResponse> resultado = fuelPerformanceService.obtenerRendimiento(
                "MAQUINARIA", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        Map<Long, FuelPerformanceResponse> porId = resultado.stream()
                .collect(java.util.stream.Collectors.toMap(FuelPerformanceResponse::refuelingId, f -> f));
        assertEquals(4, resultado.size());

        // T2 y T3: todavía sin las 2 desviaciones previas mínimas -> tolerancia fija (15%).
        assertFalse(porId.get(2L).usaRangoAprendido());
        assertFalse(porId.get(2L).alerta()); // |1| > 10*0.15=1.5 ? no
        assertFalse(porId.get(3L).usaRangoAprendido());
        assertTrue(porId.get(3L).alerta()); // |2| > 10*0.15=1.5 ? sí

        // T4: ya hay 2 desviaciones previas (0.1, 0.2) -> rango aprendido.
        // μ=0.15, σ_muestral=sqrt(((0.1-0.15)^2+(0.2-0.15)^2)/1)=sqrt(0.005)≈0.0707
        // |0.3-0.15|=0.15 > 1.5*0.0707≈0.1061 -> alerta
        assertTrue(porId.get(4L).usaRangoAprendido());
        assertTrue(porId.get(4L).alerta());

        // T5: 3 desviaciones previas (0.1, 0.2, 0.3) -> μ=0.2, σ_muestral=0.1
        // |0.16-0.2|=0.04 <= 1.5*0.1=0.15 -> dentro del rango propio del activo, sin alerta
        // (aunque en términos absolutos su diferencia de 1.6 gal ya no dispararía ni el 15% fijo,
        // lo importante es que usa el método aprendido, no el fijo).
        assertTrue(porId.get(5L).usaRangoAprendido());
        assertFalse(porId.get(5L).alerta());
    }

    @Test
    void conMenosDeDosDesviacionesPrevias_NuncaUsaElRangoAprendido() {
        // Mismo activo que la config por defecto de los tests de arriba: con exactamente
        // 1 tanqueo previo (0 desviaciones previas comparables), sigue en modo fijo.
        RefuelingRecordsEntity t1 = tanqueoMaquina(1, 30, "0", "0", LocalDateTime.of(2026, 1, 1, 8, 0));
        RefuelingRecordsEntity t2 = tanqueoMaquina(2, 30, "10", "11", LocalDateTime.of(2026, 1, 2, 8, 0));

        when(refuelingRecordsRepository.findByMachineIdIsNotNullAndFechaRegistroBetween(any(), any()))
                .thenReturn(List.of(t2));
        when(refuelingRecordsRepository.findByMachineIdOrderByFechaRegistroAsc(30L))
                .thenReturn(List.of(t1, t2));
        when(assetFuelConfigRepository.findByMachineId(30L)).thenReturn(Optional.of(
                AssetFuelConfigEntity.builder().machineId(30L).consumoEstandar(new BigDecimal("1")).unidadConsumo("HORA_POR_GALON").build()));

        List<FuelPerformanceResponse> resultado = fuelPerformanceService.obtenerRendimiento(
                "MAQUINARIA", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertEquals(1, resultado.size());
        assertFalse(resultado.get(0).usaRangoAprendido());
    }
}
