package com.app.usochicamochabackend.maintenance.web;

import com.app.usochicamochabackend.auth.utils.JwtUtils;
import com.app.usochicamochabackend.maintenance.application.dto.MaintenanceRequest;
import com.app.usochicamochabackend.maintenance.application.dto.MaintenanceResponse;
import com.app.usochicamochabackend.maintenance.application.port.MaintenanceUseCase;
import com.app.usochicamochabackend.update.application.service.ExcelGenerationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MaintenanceController.class)
@AutoConfigureMockMvc
class MaintenanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MaintenanceUseCase maintenanceUseCase;

    @MockBean
    private ExcelGenerationService excelGenerationService;

    @MockBean
    private JwtUtils jwtUtils;

    private MaintenanceResponse buildResponse() {
        return new MaintenanceResponse(1L, LocalDateTime.now(), "ABC123", "Bodega Central",
                "Técnico", 15000, "PREVENTIVO", "Filtro aceite", "Taller X", "Sin novedad");
    }

    @Test
    @DisplayName("POST /api/v1/maintenance: registra mantenimiento → 201")
    @WithMockUser(roles = "ADMIN")
    void registerMaintenance_DatosValidos_Devuelve201() throws Exception {
        doNothing().when(maintenanceUseCase).registerMaintenance(any(MaintenanceRequest.class));

        MaintenanceRequest request = new MaintenanceRequest(
                LocalDateTime.now(), 1, 1, "Técnico", 15000, "PREVENTIVO", "Filtro", "Taller", "OK");

        mockMvc.perform(post("/api/v1/maintenance")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET /api/v1/maintenance/motos: retorna mantenimientos de motos")
    @WithMockUser
    void getMotosMaintenance_RetornaLista() throws Exception {
        when(maintenanceUseCase.getMotosMaintenance()).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/v1/maintenance/motos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].placa").value("ABC123"));
    }

    @Test
    @DisplayName("GET /api/v1/maintenance/vehicles: retorna mantenimientos de vehículos")
    @WithMockUser
    void getVehiclesMaintenance_RetornaLista() throws Exception {
        when(maintenanceUseCase.getVehiclesMaintenance()).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/v1/maintenance/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
