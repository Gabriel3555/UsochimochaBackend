package com.app.usochicamochabackend.vehicle.application.service;

import com.app.usochicamochabackend.vehicle.application.dto.VehicleMonitoringDTO;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.repository.VehicleRepository;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.entity.DocumentacionYElementosEntity;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.entity.InspPreOperativaEntity;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.DocumentacionYElementosRepository;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.InspPreOperativaRepository;
import com.app.usochicamochabackend.update.infrastructure.entity.VehicleOilChangeEntity;
import com.app.usochicamochabackend.update.infrastructure.entity.OilType;
import com.app.usochicamochabackend.update.infrastructure.repository.VehicleOilChangeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleMonitoringServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private InspPreOperativaRepository inspectionRepository;

    @Mock
    private DocumentacionYElementosRepository documentRepository;

    @Mock
    private VehicleOilChangeRepository oilChangeRepository;

    @InjectMocks
    private VehicleMonitoringService vehicleMonitoringService;

    private VehicleEntity testVehicle;
    private VehicleOilChangeEntity testOilChange;
    private InspPreOperativaEntity testInspection;

    @BeforeEach
    void setUp() {
        testVehicle = new VehicleEntity();
        testVehicle.setIdVehiculo(1);
        testVehicle.setPlaca("ABC123");
        testVehicle.setBelongsTo("Distrito");
        testVehicle.setKilometrajeActual(33050);
        testVehicle.setFechaUltimoReporte(LocalDateTime.now());

        testInspection = new InspPreOperativaEntity();
        testInspection.setIdVehiculo(1);
        testInspection.setFechaRegistro(LocalDateTime.now());

        testOilChange = new VehicleOilChangeEntity();
        testOilChange.setKmAtChange(30000);
        testOilChange.setIntervalKm(3000);
        testOilChange.setOilType(OilType.MOTOR);
        testOilChange.setDateStamp(LocalDateTime.now());
        testOilChange.setQuantity(4.0);
    }

    @Test
    void testConsolidatedMonitoringReturnsNotNull() {
        when(vehicleRepository.findAllByTipoNameNot("MOTOCICLETA")).thenReturn(java.util.Collections.emptyList());

        var result = vehicleMonitoringService.getConsolidatedMonitoring();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
