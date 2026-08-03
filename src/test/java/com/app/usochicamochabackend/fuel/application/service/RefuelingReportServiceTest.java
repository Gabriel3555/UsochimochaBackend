package com.app.usochicamochabackend.fuel.application.service;

import com.app.usochicamochabackend.catalog.infrastructure.entity.TipoVehiculoEntity;
import com.app.usochicamochabackend.fuel.application.dto.RefuelingRecordResponse;
import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.RefuelingRecordsRepository;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefuelingReportServiceTest {

    @Mock
    private RefuelingRecordsRepository refuelingRecordsRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private AssetFuelCapacityService assetFuelCapacityService;

    private RefuelingReportService refuelingReportService;

    @BeforeEach
    void setUp() {
        refuelingReportService = new RefuelingReportService(refuelingRecordsRepository, vehicleRepository, assetFuelCapacityService);
    }

    private VehicleEntity vehiculo(int id, String tipoNombre) {
        return VehicleEntity.builder().idVehiculo(id)
                .tipoVehiculo(TipoVehiculoEntity.builder().nombreTipo(tipoNombre).build())
                .build();
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
    void tipoVehiculo_ExcluyeMotos() {
        RefuelingRecordsEntity camioneta = tanqueoVehiculo(5, "DISTRITO");
        RefuelingRecordsEntity moto = tanqueoVehiculo(6, "DISTRITO");
        when(refuelingRecordsRepository.findByVehicleIdIsNotNullAndFechaRegistroBetween(any(), any()))
                .thenReturn(List.of(camioneta, moto));
        when(vehicleRepository.findById(5)).thenReturn(Optional.of(vehiculo(5, "Camioneta")));
        when(vehicleRepository.findById(6)).thenReturn(Optional.of(vehiculo(6, "Motocicleta")));

        List<RefuelingRecordResponse> resultado = refuelingReportService.obtenerReporte("VEHICULO", null, null, null);

        assertEquals(1, resultado.size());
        assertEquals(5, resultado.get(0).vehicleId());
    }

    @Test
    void tipoMaquinariaMoto_IncluyeMaquinasYMotosNoVehiculosNormales() {
        RefuelingRecordsEntity maquina = tanqueoMaquina(10L);
        RefuelingRecordsEntity moto = tanqueoVehiculo(6, "DISTRITO");
        RefuelingRecordsEntity camioneta = tanqueoVehiculo(5, "DISTRITO");
        when(refuelingRecordsRepository.findByMachineIdIsNotNullAndFechaRegistroBetween(any(), any()))
                .thenReturn(List.of(maquina));
        when(refuelingRecordsRepository.findByVehicleIdIsNotNullAndFechaRegistroBetween(any(), any()))
                .thenReturn(List.of(moto, camioneta));
        when(vehicleRepository.findById(6)).thenReturn(Optional.of(vehiculo(6, "Motocicleta")));
        when(vehicleRepository.findById(5)).thenReturn(Optional.of(vehiculo(5, "Camioneta")));

        List<RefuelingRecordResponse> resultado = refuelingReportService.obtenerReporte("MAQUINARIA_MOTO", null, null, null);

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().anyMatch(r -> Long.valueOf(10L).equals(r.machineId())));
        assertTrue(resultado.stream().anyMatch(r -> Integer.valueOf(6).equals(r.vehicleId())));
        assertTrue(resultado.stream().noneMatch(r -> Integer.valueOf(5).equals(r.vehicleId())));
    }

    @Test
    void filtraPorAreaCuandoNoEsTodas() {
        RefuelingRecordsEntity distrito = tanqueoVehiculo(5, "DISTRITO");
        RefuelingRecordsEntity asociacion = tanqueoVehiculo(6, "ASOCIACION");
        when(refuelingRecordsRepository.findByVehicleIdIsNotNullAndFechaRegistroBetween(any(), any()))
                .thenReturn(List.of(distrito, asociacion));
        when(vehicleRepository.findById(5)).thenReturn(Optional.of(vehiculo(5, "Camioneta")));
        when(vehicleRepository.findById(6)).thenReturn(Optional.of(vehiculo(6, "Camioneta")));

        List<RefuelingRecordResponse> resultado = refuelingReportService.obtenerReporte("VEHICULO", "DISTRITO", null, null);

        assertEquals(1, resultado.size());
        assertEquals("DISTRITO", resultado.get(0).areaCosto());
    }

    @Test
    void areaTodasONula_NoFiltra() {
        RefuelingRecordsEntity distrito = tanqueoVehiculo(5, "DISTRITO");
        RefuelingRecordsEntity asociacion = tanqueoVehiculo(6, "ASOCIACION");
        when(refuelingRecordsRepository.findByVehicleIdIsNotNullAndFechaRegistroBetween(any(), any()))
                .thenReturn(List.of(distrito, asociacion));
        when(vehicleRepository.findById(5)).thenReturn(Optional.of(vehiculo(5, "Camioneta")));
        when(vehicleRepository.findById(6)).thenReturn(Optional.of(vehiculo(6, "Camioneta")));

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
        when(refuelingRecordsRepository.findByVehicleIdIsNotNullAndFechaRegistroBetween(any(), any()))
                .thenReturn(List.of(camioneta));
        when(vehicleRepository.findById(5)).thenReturn(Optional.of(vehiculo(5, "Camioneta")));
        when(assetFuelCapacityService.excedeCapacidad(5, null, new BigDecimal("10"))).thenReturn(true);

        List<RefuelingRecordResponse> resultado = refuelingReportService.obtenerReporte("VEHICULO", null, null, null);

        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).capacidadExcedida());
    }
}
