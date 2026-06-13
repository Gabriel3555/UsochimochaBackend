package com.app.usochicamochabackend.moto.application.service;

import com.app.usochicamochabackend.moto.application.dto.MotoMonitoringDTO;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.DocumentacionYElementosRepository;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.InspPreOperativaRepository;
import com.app.usochicamochabackend.update.infrastructure.entity.VehicleOilChangeEntity;
import com.app.usochicamochabackend.update.infrastructure.entity.OilType;
import com.app.usochicamochabackend.update.infrastructure.repository.VehicleOilChangeRepository;
import com.app.usochicamochabackend.catalog.infrastructure.repository.UbicacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MotoMonitoringServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private InspPreOperativaRepository inspectionRepository;

    @Mock
    private DocumentacionYElementosRepository documentRepository;

    @Mock
    private VehicleOilChangeRepository oilChangeRepository;

    @Mock
    private UbicacionRepository ubicacionRepository;

    @InjectMocks
    private MotoMonitoringService motoMonitoringService;

    private VehicleEntity testMoto;
    private VehicleOilChangeEntity testOilChange;

    @BeforeEach
    void setUp() {
        testMoto = new VehicleEntity();
        testMoto.setIdVehiculo(1);
        testMoto.setPlaca("SDZ53F");
        testMoto.setBelongsTo("Distrito");
        testMoto.setKilometrajeActual(33050);
        testMoto.setFechaUltimoReporte(LocalDateTime.now());

        testOilChange = new VehicleOilChangeEntity();
        testOilChange.setKmAtChange(30200);
        testOilChange.setIntervalKm(3000);
        testOilChange.setOilType(OilType.MOTOR);
        testOilChange.setDateStamp(LocalDateTime.now());
        testOilChange.setQuantity(1.5);
        testOilChange.setAirFilterChanged(false);
    }

    @Test
    void testConsolidatedMonitoringReturnsNotNull() {
        when(vehicleRepository.findAllByTipoName("MOTOCICLETA")).thenReturn(java.util.Collections.emptyList());

        var result = motoMonitoringService.getConsolidatedMonitoring();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
