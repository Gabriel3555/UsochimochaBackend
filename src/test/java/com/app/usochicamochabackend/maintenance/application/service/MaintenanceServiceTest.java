package com.app.usochicamochabackend.maintenance.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.maintenance.application.dto.MaintenanceRequest;
import com.app.usochicamochabackend.maintenance.infrastructure.entity.MaintenanceEntity;
import com.app.usochicamochabackend.maintenance.infrastructure.repository.MaintenanceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceServiceTest {

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @Mock
    private SaveActionUseCase saveActionUseCase;

    @InjectMocks
    private MaintenanceService maintenanceService;

    @BeforeEach
    void setupSecurityContext() {
        UserPrincipal principal = new UserPrincipal(1L, "tecnico");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private MaintenanceRequest buildRequest(Integer idVehiculo) {
        return new MaintenanceRequest(
                LocalDateTime.now(), idVehiculo, 1, "Resp", 15000,
                "PREVENTIVO", "Filtro aceite", "Taller X", "Sin novedad");
    }

    @Test
    @DisplayName("registerMaintenance: guarda entidad y registra acción")
    void registerMaintenance_GuardaYRegistraAccion() {
        MaintenanceEntity saved = MaintenanceEntity.builder()
                .id(1L).idVehiculo(5).tipoMantenimiento("PREVENTIVO").build();
        when(maintenanceRepository.save(any())).thenReturn(saved);

        maintenanceService.registerMaintenance(buildRequest(5));

        verify(maintenanceRepository).save(any(MaintenanceEntity.class));
        verify(saveActionUseCase).save(anyString());
    }

    @Test
    @DisplayName("registerMaintenance: fecha null → usa LocalDateTime.now()")
    void registerMaintenance_FechaNull_UsaFechaActual() {
        MaintenanceRequest request = new MaintenanceRequest(
                null, 2, 1, "Resp", 10000, "CORRECTIVO", null, null, null);
        MaintenanceEntity saved = MaintenanceEntity.builder().id(1L).idVehiculo(2).tipoMantenimiento("CORRECTIVO").build();
        when(maintenanceRepository.save(any())).thenReturn(saved);

        maintenanceService.registerMaintenance(request);

        verify(maintenanceRepository).save(argThat(entity -> entity.getFecha() != null));
    }

    @Test
    @DisplayName("getMotosMaintenance: consulta repositorio con tipo MOTOCICLETA")
    void getMotosMaintenance_ConsultaRepositorio() {
        when(maintenanceRepository.findAllByVehicleTypeName("MOTOCICLETA")).thenReturn(List.of());

        var result = maintenanceService.getMotosMaintenance();

        assertNotNull(result);
        verify(maintenanceRepository).findAllByVehicleTypeName("MOTOCICLETA");
    }
}
