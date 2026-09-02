package com.app.usochicamochabackend.update.web;

import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.auth.utils.JwtUtils;
import com.app.usochicamochabackend.update.application.dto.MachineOilChangeHistoryDTO;
import com.app.usochicamochabackend.update.application.port.GetConsolidateHydraulicAndMotorOilAllMachinesUseCase;
import com.app.usochicamochabackend.update.application.port.ManageMachineOilChangeHistoryUseCase;
import com.app.usochicamochabackend.update.application.port.PerformHydraulicChangeUseCase;
import com.app.usochicamochabackend.update.application.port.PerformMotorOilChangeUseCase;
import com.app.usochicamochabackend.update.application.service.ExcelGenerationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OilChangeController.class)
@AutoConfigureMockMvc
class OilChangeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetConsolidateHydraulicAndMotorOilAllMachinesUseCase getConsolidateHydraulicAndMotorOilAllMachines;

    @MockBean
    private PerformMotorOilChangeUseCase performMotorOilChange;

    @MockBean
    private PerformHydraulicChangeUseCase performHydraulicChange;

    @MockBean
    private ExcelGenerationService excelGenerationService;

    @MockBean
    private ManageMachineOilChangeHistoryUseCase manageMachineOilChangeHistoryUseCase;

    @MockBean
    private JwtUtils jwtUtils;

    private static RequestPostProcessor as(Long id, String username, String role) {
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
        Authentication auth = new UsernamePasswordAuthenticationToken(new UserPrincipal(id, username), null, List.of(authority));
        return authentication(auth);
    }

    private static final String BODY = """
            {"machineId":1,"dateTime":"2026-08-01T10:00:00","brandId":2,
             "quantity":4.0,"currentHourMeter":700.0,"averageHoursChange":250}""";

    // Nota: la matriz de permisos real se prueba con contexto completo — @PreAuthorize
    // no se aplica en el slice @WebMvcTest (mismo criterio que RefuelingRecordControllerTest).

    @Test
    @DisplayName("GET /oil-changes/machine/{machineId}/history: devuelve 200 con el historial")
    void getHistory_Devuelve200() throws Exception {
        when(manageMachineOilChangeHistoryUseCase.obtenerHistorial(1L, "MOTOR")).thenReturn(List.of(
                new MachineOilChangeHistoryDTO(10L, LocalDateTime.now(), "MOTOR", 2L, "Mobil", 4.0, 700.0, 250)));

        mockMvc.perform(get("/api/v1/oil-changes/machine/{machineId}/history", 1L)
                        .param("tipo", "MOTOR")
                        .with(as(3L, "admin", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].brandName").value("Mobil"));
    }

    @Test
    @DisplayName("PUT /oil-changes/{id}: devuelve 204")
    void actualizar_Devuelve204() throws Exception {
        doNothing().when(manageMachineOilChangeHistoryUseCase).actualizarCambioAceite(anyLong(), any());

        mockMvc.perform(put("/api/v1/oil-changes/{id}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY)
                        .with(csrf())
                        .with(as(3L, "admin", "ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /oil-changes/{id}: devuelve 204")
    void eliminar_Devuelve204() throws Exception {
        doNothing().when(manageMachineOilChangeHistoryUseCase).eliminarCambioAceite(10L);

        mockMvc.perform(delete("/api/v1/oil-changes/{id}", 10L)
                        .with(csrf())
                        .with(as(3L, "admin", "ADMIN")))
                .andExpect(status().isNoContent());
    }
}
