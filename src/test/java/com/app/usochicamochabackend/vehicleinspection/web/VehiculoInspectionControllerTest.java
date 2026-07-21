package com.app.usochicamochabackend.vehicleinspection.web;

import com.app.usochicamochabackend.auth.utils.JwtUtils;
import com.app.usochicamochabackend.update.application.service.ExcelGenerationService;
import com.app.usochicamochabackend.vehicleinspection.application.dto.VehicleInspectionReportDTO;
import com.app.usochicamochabackend.vehicleinspection.application.service.VehiculoInspectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VehiculoInspectionController.class)
@AutoConfigureMockMvc
class VehiculoInspectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VehiculoInspectionService vehiculoInspectionService;

    @MockBean
    private ExcelGenerationService excelGenerationService;

    @MockBean
    private JwtUtils jwtUtils;

    @Test
    @DisplayName("GET /api/v1/vehicle-inspection/reports/all: retorna todas las inspecciones de vehículos")
    @WithMockUser
    void getAllVehicleInspections_RetornaLista() throws Exception {
        when(vehiculoInspectionService.getAllVehicleInspections()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/vehicle-inspection/reports/all"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/vehicle-inspection/reports/latest: retorna inspecciones más recientes por vehículo")
    @WithMockUser
    void getLatestVehicleInspections_RetornaLista() throws Exception {
        when(vehiculoInspectionService.getLatestVehicleInspectionsPerVehicle()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/vehicle-inspection/reports/latest"))
                .andExpect(status().isOk());
    }
}
