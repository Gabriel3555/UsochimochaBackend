package com.app.usochicamochabackend.moto.web;

import com.app.usochicamochabackend.auth.utils.JwtUtils;
import com.app.usochicamochabackend.moto.application.dto.*;
import com.app.usochicamochabackend.update.application.service.ExcelGenerationService;
import com.app.usochicamochabackend.moto.application.port.MotoMonitoringUseCase;
import com.app.usochicamochabackend.moto.application.service.MotoService;
import com.app.usochicamochabackend.vehicleinspection.application.port.GetVehicleInspectionsUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MotoController.class)
@AutoConfigureMockMvc
class MotoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MotoService motoService;

    @MockBean
    private MotoMonitoringUseCase motoMonitoringUseCase;

    @MockBean
    private GetVehicleInspectionsUseCase getVehicleInspectionsUseCase;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private ExcelGenerationService excelGenerationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void getMotocicletas_ShouldReturnOk() throws Exception {
        when(motoService.getMotocicletas()).thenReturn(
                List.of(new MotoPlacaResponse(1, "XYZ789", 2, "Unidad Pantano")));

        mockMvc.perform(get("/api/v1/moto/placas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].placa").value("XYZ789"))
                .andExpect(jsonPath("$[0].idUbicacionBase").value(2))
                .andExpect(jsonPath("$[0].ubicacionBase").value("Unidad Pantano"));
    }

    @Test
    @WithMockUser
    void getUbicaciones_ShouldReturnOk() throws Exception {
        when(motoService.getUbicaciones()).thenReturn(List.of(new UbicacionResponse(1, "Duitama")));

        mockMvc.perform(get("/api/v1/moto/ubicaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreUbicacion").value("Duitama"));
    }

    @Test
    @WithMockUser
    void getMotoInspectionsLatest_ShouldReturnOk() throws Exception {
        when(getVehicleInspectionsUseCase.getMotoInspectionsLatestPerVehicle()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/moto/inspections/reports"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getMotoInspectionsHistory_ShouldReturnOk() throws Exception {
        when(getVehicleInspectionsUseCase.getMotoInspectionsHistory()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/moto/inspections/reports/history"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void saveInspeccion_ShouldReturnId() throws Exception {
        InspeccionMotoRequest request = new InspeccionMotoRequest(
                1, 5000, "BUENO", "Todo ok",
                "Vigente", "Vigente", "Vigente", "N/A",
                "Bueno", "Bueno", "Bueno",
                1);

        when(motoService.saveInspeccion(any())).thenReturn(100L);

        mockMvc.perform(post("/api/v1/moto/inspeccion")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").value(100L));
    }
}
