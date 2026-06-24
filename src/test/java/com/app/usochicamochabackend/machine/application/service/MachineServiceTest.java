package com.app.usochicamochabackend.machine.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.machine.application.dto.MachineRequest;
import com.app.usochicamochabackend.machine.application.dto.MachineResponse;
import com.app.usochicamochabackend.machine.infrastructure.entity.MachineEntity;
import com.app.usochicamochabackend.machine.infrastructure.repository.MachineRepository;
import com.app.usochicamochabackend.notifications.application.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MachineServiceTest {

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private SaveActionUseCase saveActionUseCase;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private MachineService machineService;

    private MachineRequest buildRequest(String name) {
        return new MachineRequest(name, "Usochicamocha", "2020", null, "Toyota",
                null, "ENG001", "VIN001", BigDecimal.valueOf(80), BigDecimal.valueOf(5), "GAL/H");
    }

    private MachineEntity buildEntity(Long id, String name, boolean status) {
        return MachineEntity.builder().id(id).name(name).status(status)
                .brand("Toyota").model("2020").build();
    }

    @Test
    @DisplayName("createMachine: guarda y retorna respuesta correcta")
    void createMachine_GuardaYRetornaRespuesta() {
        MachineEntity saved = buildEntity(10L, "Excavadora", true);
        when(machineRepository.save(any())).thenReturn(saved);

        MachineResponse result = machineService.createMachine(buildRequest("excavadora"));

        assertNotNull(result);
        assertEquals(10L, result.id());
        assertEquals("Excavadora", result.name());
        verify(saveActionUseCase).save(anyString());
    }

    @Test
    @DisplayName("findAllMachines: retorna solo activas")
    void findAllMachines_RetornaSoloActivas() {
        MachineEntity active = buildEntity(1L, "Retroexcavadora", true);
        MachineEntity inactive = buildEntity(2L, "Niveladora", false);
        when(machineRepository.findAll()).thenReturn(List.of(active, inactive));

        List<MachineResponse> result = machineService.findAllMachines();

        assertEquals(1, result.size());
        assertEquals("Retroexcavadora", result.get(0).name());
    }

    @Test
    @DisplayName("findMachineById: máquina activa existente → retorna respuesta")
    void findMachineById_Activa_RetornaRespuesta() {
        MachineEntity entity = buildEntity(5L, "Bulldozer", true);
        when(machineRepository.findById(5L)).thenReturn(Optional.of(entity));

        MachineResponse result = machineService.findMachineById(5L);

        assertEquals("Bulldozer", result.name());
        verify(saveActionUseCase).save(anyString());
    }

    @Test
    @DisplayName("findMachineById: máquina no encontrada → lanza ResourceNotFoundException")
    void findMachineById_NoExiste_LanzaExcepcion() {
        when(machineRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> machineService.findMachineById(99L));
    }

    @Test
    @DisplayName("findMachineById: máquina inactiva → lanza ResourceNotFoundException")
    void findMachineById_Inactiva_LanzaExcepcion() {
        MachineEntity inactive = buildEntity(3L, "Grúa", false);
        when(machineRepository.findById(3L)).thenReturn(Optional.of(inactive));
        assertThrows(ResourceNotFoundException.class, () -> machineService.findMachineById(3L));
    }

    @Test
    @DisplayName("updateMachine: máquina activa → actualiza y retorna")
    void updateMachine_Activa_Actualiza() {
        MachineEntity current = buildEntity(1L, "Moto Bomba", true);
        MachineEntity updated = buildEntity(1L, "Moto Bomba Honda", true);

        when(machineRepository.findById(1L)).thenReturn(Optional.of(current));
        when(machineRepository.save(any())).thenReturn(updated);

        MachineResponse result = machineService.updateMachine(buildRequest("moto bomba honda"), 1L);

        assertEquals("Moto Bomba Honda", result.name());
        verify(saveActionUseCase).save(anyString());
    }

    @Test
    @DisplayName("updateMachine: máquina no encontrada → lanza ResourceNotFoundException")
    void updateMachine_NoExiste_LanzaExcepcion() {
        when(machineRepository.findById(77L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> machineService.updateMachine(buildRequest("Test"), 77L));
    }

    @Test
    @DisplayName("deleteMachine: máquina activa → soft delete")
    void deleteMachine_Activa_SoftDelete() {
        MachineEntity entity = buildEntity(2L, "Compactadora", true);
        when(machineRepository.findById(2L)).thenReturn(Optional.of(entity));
        when(machineRepository.save(any())).thenReturn(entity);

        // deleteMachine accede al SecurityContextHolder directamente; necesita contexto.
        // Configuramos un contexto mínimo con UserPrincipal antes de invocar.
        com.app.usochicamochabackend.auth.application.dto.UserPrincipal principal =
                new com.app.usochicamochabackend.auth.application.dto.UserPrincipal(1L, "admin");
        org.springframework.security.core.Authentication auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principal, null, java.util.List.of());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            machineService.deleteMachine(2L);
            assertFalse(entity.getStatus());
            verify(machineRepository).save(entity);
            verify(saveActionUseCase).save(anyString());
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("deleteMachine: máquina no encontrada → lanza ResourceNotFoundException")
    void deleteMachine_NoExiste_LanzaExcepcion() {
        when(machineRepository.findById(88L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> machineService.deleteMachine(88L));
    }

    @Test
    @DisplayName("deleteMachine: máquina inactiva → lanza ResourceNotFoundException")
    void deleteMachine_Inactiva_LanzaExcepcion() {
        MachineEntity inactive = buildEntity(4L, "Tractor", false);
        when(machineRepository.findById(4L)).thenReturn(Optional.of(inactive));
        assertThrows(ResourceNotFoundException.class, () -> machineService.deleteMachine(4L));
    }
}
