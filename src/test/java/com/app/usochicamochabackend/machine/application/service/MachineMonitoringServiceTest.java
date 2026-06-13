package com.app.usochicamochabackend.machine.application.service;

import com.app.usochicamochabackend.machine.application.dto.MachineMonitoringDTO;
import com.app.usochicamochabackend.machine.infrastructure.entity.MachineEntity;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
import com.app.usochicamochabackend.update.infrastructure.entity.OilChangeEntity;
import com.app.usochicamochabackend.update.infrastructure.repository.OilChangeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MachineMonitoringService Tests")
class MachineMonitoringServiceTest {

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private OilChangeRepository oilChangeRepository;

    private MachineMonitoringService machineMonitoringService;

    @BeforeEach
    void setUp() {
        machineMonitoringService = new MachineMonitoringService(machineRepository, oilChangeRepository);
    }

    @Test
    @DisplayName("getConsolidatedMonitoring devuelve lista de máquinas activas")
    void testGetConsolidatedMonitoring() {
        MachineEntity machine = createMockMachine(1L, "Excavadora-01", 500);
        machine.setStatus(true);

        when(machineRepository.findAll()).thenReturn(List.of(machine));
        when(oilChangeRepository.getLastMotorOilChangeByMachineId(1L)).thenReturn(null);
        when(oilChangeRepository.getLastHydraulicOilChangeByMachineId(1L)).thenReturn(null);

        List<MachineMonitoringDTO> result = machineMonitoringService.getConsolidatedMonitoring();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Excavadora-01", result.get(0).machineName());
    }

    @Test
    @DisplayName("Máquina con alerta YELLOW (80% intervalo)")
    void testMachineWithYellowAlert() {
        MachineEntity machine = createMockMachine(1L, "Excavadora-02", 400);
        machine.setStatus(true);

        OilChangeEntity motorOil = new OilChangeEntity();
        motorOil.setHourStamp(0);
        motorOil.setAverageHoursChange(500);

        when(machineRepository.findAll()).thenReturn(List.of(machine));
        when(oilChangeRepository.getLastMotorOilChangeByMachineId(1L)).thenReturn(motorOil);
        when(oilChangeRepository.getLastHydraulicOilChangeByMachineId(1L)).thenReturn(null);

        List<MachineMonitoringDTO> result = machineMonitoringService.getConsolidatedMonitoring();

        assertEquals("YELLOW", result.get(0).motorOilInfo().alertColor());
        assertEquals(80.0, result.get(0).motorOilInfo().percentageUsed(), 0.1);
    }

    @Test
    @DisplayName("Máquina con alerta RED (vencida)")
    void testMachineWithRedAlert() {
        MachineEntity machine = createMockMachine(1L, "Excavadora-03", 550);
        machine.setStatus(true);

        OilChangeEntity motorOil = new OilChangeEntity();
        motorOil.setHourStamp(0);
        motorOil.setAverageHoursChange(500);

        when(machineRepository.findAll()).thenReturn(List.of(machine));
        when(oilChangeRepository.getLastMotorOilChangeByMachineId(1L)).thenReturn(motorOil);
        when(oilChangeRepository.getLastHydraulicOilChangeByMachineId(1L)).thenReturn(null);

        List<MachineMonitoringDTO> result = machineMonitoringService.getConsolidatedMonitoring();

        assertEquals("RED", result.get(0).motorOilInfo().alertColor());
        assertTrue(result.get(0).motorOilInfo().percentageUsed() > 100);
    }

    private MachineEntity createMockMachine(Long id, String name, Integer horometro) {
        MachineEntity machine = new MachineEntity();
        machine.setId(id);
        machine.setName(name);
        machine.setHorometroActual(horometro);
        return machine;
    }
}
