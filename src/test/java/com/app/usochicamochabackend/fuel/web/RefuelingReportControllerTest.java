package com.app.usochicamochabackend.fuel.web;

import com.app.usochicamochabackend.fuel.application.dto.RefuelingRecordResponse;
import com.app.usochicamochabackend.fuel.application.port.ExportRefuelingReportUseCase;
import com.app.usochicamochabackend.fuel.application.port.GetRefuelingReportUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RefuelingReportController.class)
@AutoConfigureMockMvc
class RefuelingReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetRefuelingReportUseCase getRefuelingReportUseCase;

    @MockBean
    private ExportRefuelingReportUseCase exportRefuelingReportUseCase;

    @Test
    @DisplayName("GET /fuel/refueling/reporte: SUPERVISOR_OPERATIVO autorizado, devuelve 200")
    @WithMockUser(roles = "SUPERVISOR_OPERATIVO")
    void reporte_ConRolSupervisorOperativo_Devuelve200() throws Exception {
        when(getRefuelingReportUseCase.obtenerReporte(any(), any(), any(), any())).thenReturn(List.<RefuelingRecordResponse>of());

        mockMvc.perform(get("/api/v1/fuel/refueling/reporte").param("tipo", "VEHICULO"))
                .andExpect(status().isOk());
    }
}
