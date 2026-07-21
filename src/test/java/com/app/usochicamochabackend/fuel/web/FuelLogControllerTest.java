package com.app.usochicamochabackend.fuel.web;

import com.app.usochicamochabackend.auth.utils.JwtUtils;
import com.app.usochicamochabackend.fuel.application.dto.FuelLogRequest;
import com.app.usochicamochabackend.fuel.application.dto.FuelLogResponse;
import com.app.usochicamochabackend.fuel.application.port.CreateFuelLogUseCase;
import com.app.usochicamochabackend.fuel.application.port.GetFuelDashboardUseCase;
import com.app.usochicamochabackend.fuel.application.port.GetFuelHistoryUseCase;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FuelLogController.class)
@AutoConfigureMockMvc
class FuelLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateFuelLogUseCase createFuelLogUseCase;

    @MockBean
    private GetFuelHistoryUseCase getFuelHistoryUseCase;

    @MockBean
    private GetFuelDashboardUseCase getFuelDashboardUseCase;

    @MockBean
    private JwtUtils jwtUtils;

    private FuelLogResponse buildResponse() {
        return new FuelLogResponse(1L, "VEHICLE", 1L, "ABC123", LocalDateTime.now(),
                null, null, null, null, null, null, BigDecimal.valueOf(10), "GALLONS",
                BigDecimal.valueOf(10), BigDecimal.valueOf(15000), BigDecimal.valueOf(150000),
                BigDecimal.valueOf(150000), false, "DIESEL", "Estación Central",
                BigDecimal.ZERO, null, "PENDING", "V001",
                null, null, null, false, "admin", LocalDateTime.now(), null, null, null,
                null, null);
    }

    @Test
    @DisplayName("GET /api/v1/fuel/asset/{assetType}/{assetId}: retorna historial del activo")
    @WithMockUser
    void getFuelHistory_RetornaLista() throws Exception {
        when(getFuelHistoryUseCase.getHistoryByAsset(eq("VEHICLE"), eq(1L))).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/v1/fuel/asset/VEHICLE/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/fuel/{id}: retorna registro específico")
    @WithMockUser
    void getFuelById_RetornaRegistro() throws Exception {
        when(getFuelHistoryUseCase.getById(1L)).thenReturn(buildResponse());

        mockMvc.perform(get("/api/v1/fuel/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}
