package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.fuel.application.dto.RefuelingRecordResponse;
import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelReintegrationsRepository;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefuelingReportServiceTest {

    @Mock
    private RefuelingRecordsRepository refuelingRecordsRepository;

    @Mock
    private AssetFuelCapacityService assetFuelCapacityService;

    @Mock
    private FuelPriceAnomalyService fuelPriceAnomalyService;

    @Mock
    private FuelFullConsistencyService fuelFullConsistencyService;

    @Mock
    private FuelReintegrationsRepository fuelReintegrationsRepository;

    private RefuelingReportService refuelingReportService;

    @BeforeEach
    void setUp() {
        refuelingReportService = new RefuelingReportService(
                refuelingRecordsRepository, assetFuelCapacityService, fuelPriceAnomalyService,
                fuelFullConsistencyService, fuelReintegrationsRepository);
    }

    private RefuelingRecordsEntity tanqueoVehiculo(int vehicleId, String areaCosto) {
        return RefuelingRecordsEntity.builder().id((long) vehicleId).vehicleId(vehicleId).areaCosto(areaCosto)
                .cantidadGalones(new BigDecimal("10")).fuelTypeId(1L)
                .fechaRegistro(Timestamp.valueOf(LocalDateTime.of(2026, 7, 15, 10, 0)))
                .build();
    }

    private RefuelingRecordsEntity tanqueoMaquina(long machineId) {
        return RefuelingRecordsEntity.builder().id(machineId).machineId(machineId).areaCosto("DISTRITO")
                .cantidadGalones(new BigDecimal("10")).fuelTypeId(1L)
                .fechaRegistro(Timestamp.valueOf(LocalDateTime.of(2026, 7, 15, 10, 0)))
                .build();
    }

    @Test
    void tipoVehiculo_PideLosDeLugarBomba() {
        RefuelingRecordsEntity camioneta = tanqueoVehiculo(5, "DISTRITO");
        when(refuelingRecordsRepository.findByLugarAndFechaRegistroBetween(eq("BOMBA"), any(), any()))
                .thenReturn(List.of(camioneta));

        List<RefuelingRecordResponse> resultado = refuelingReportService.obtenerReporte("VEHICULO", null, null, null);

        assertEquals(1, resultado.size());
        assertEquals(5, resultado.get(0).vehicleId());
    }

    @Test
    void tipoMaquinariaMoto_PideLosDeLugarAlmacen() {
        RefuelingRecordsEntity maquina = tanqueoMaquina(10L);
        when(refuelingRecordsRepository.findByLugarAndFechaRegistroBetween(eq("ALMACEN"), any(), any()))
                .thenReturn(List.of(maquina));

        List<RefuelingRecordResponse> resultado = refuelingReportService.obtenerReporte("MAQUINARIA_MOTO", null, null, null);

        assertEquals(1, resultado.size());
        assertTrue(resultado.stream().anyMatch(r -> Long.valueOf(10L).equals(r.machineId())));
    }

    @Test
    void unaMotoTanqueadaEnBomba_ApareceEnVehiculoEnVezDeMaquinariaMoto() {
        // Excepción real que motivó el cambio: agrupar por lugar (no por tipo de
        // activo fijo) para que una moto tanqueada en una bomba real se vea en
        // "Estación" — antes quedaba forzada a "Almacén" siempre, sin importar el
        // lugar real del tanqueo.
        RefuelingRecordsEntity motoEnBomba = tanqueoVehiculo(6, "DISTRITO");
        when(refuelingRecordsRepository.findByLugarAndFechaRegistroBetween(eq("BOMBA"), any(), any()))
                .thenReturn(List.of(motoEnBomba));

        List<RefuelingRecordResponse> resultado = refuelingReportService.obtenerReporte("VEHICULO", null, null, null);

        assertEquals(1, resultado.size());
        assertEquals(6, resultado.get(0).vehicleId());
    }

    @Test
    void filtraPorAreaCuandoNoEsTodas() {
        RefuelingRecordsEntity distrito = tanqueoVehiculo(5, "DISTRITO");
        RefuelingRecordsEntity asociacion = tanqueoVehiculo(6, "ASOCIACION");
        when(refuelingRecordsRepository.findByLugarAndFechaRegistroBetween(eq("BOMBA"), any(), any()))
                .thenReturn(List.of(distrito, asociacion));

        List<RefuelingRecordResponse> resultado = refuelingReportService.obtenerReporte("VEHICULO", "DISTRITO", null, null);

        assertEquals(1, resultado.size());
        assertEquals("DISTRITO", resultado.get(0).areaCosto());
    }

    @Test
    void areaTodasONula_NoFiltra() {
        RefuelingRecordsEntity distrito = tanqueoVehiculo(5, "DISTRITO");
        RefuelingRecordsEntity asociacion = tanqueoVehiculo(6, "ASOCIACION");
        when(refuelingRecordsRepository.findByLugarAndFechaRegistroBetween(eq("BOMBA"), any(), any()))
                .thenReturn(List.of(distrito, asociacion));

        List<RefuelingRecordResponse> resultado = refuelingReportService.obtenerReporte("VEHICULO", "TODAS", null, null);

        assertEquals(2, resultado.size());
    }

    @Test
    void tipoInvalido_Lanza400() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> refuelingReportService.obtenerReporte("INVALIDO", null, null, null));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void filaConCantidadQueExcedeLaCapacidadDelTanque_MarcaCapacidadExcedida() {
        RefuelingRecordsEntity camioneta = tanqueoVehiculo(5, "DISTRITO");
        when(refuelingRecordsRepository.findByLugarAndFechaRegistroBetween(eq("BOMBA"), any(), any()))
                .thenReturn(List.of(camioneta));
        when(assetFuelCapacityService.excedeCapacidad(5, null, new BigDecimal("10"))).thenReturn(true);

        List<RefuelingRecordResponse> resultado = refuelingReportService.obtenerReporte("VEHICULO", null, null, null);

        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).capacidadExcedida());
    }
}
