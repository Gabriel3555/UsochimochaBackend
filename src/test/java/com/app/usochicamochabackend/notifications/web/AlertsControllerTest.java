package com.app.usochicamochabackend.notifications.web;

import com.app.usochicamochabackend.auth.application.service.UserDetailsServiceImp;
import com.app.usochicamochabackend.auth.security.SecurityConfig;
import com.app.usochicamochabackend.auth.utils.JwtUtils;
import com.app.usochicamochabackend.notifications.application.PreventiveAlertCalculationService;
import com.app.usochicamochabackend.notifications.infrastructure.repository.PreventiveAlertRepository;
import com.app.usochicamochabackend.update.infrastructure.repository.OilChangeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regresión: /api/v1/alerts (alertas preventivas) solo cargaba para ADMIN porque
 * SecurityConfig tenía reglas para /new-data/alerts (endpoint que ya no existe en ningún
 * controlador) en vez del path real /api/v1/alerts, cayendo en el catch-all ADMIN-only.
 */
@WebMvcTest(AlertsController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class AlertsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PreventiveAlertRepository alertRepository;

    @MockBean
    private PreventiveAlertCalculationService preventiveAlertCalculationService;

    @MockBean
    private OilChangeRepository oilChangeRepository;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserDetailsServiceImp userDetailsServiceImp;

    @Test
    @DisplayName("GET /api/v1/alerts: OPERARIO puede ver las alertas")
    @WithMockUser(roles = "OPERARIO")
    void getAllAlerts_Operario_Devuelve200() throws Exception {
        when(alertRepository.findByStatus(anyBoolean(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/alerts: SUPERVISOR_OPERATIVO puede ver las alertas")
    @WithMockUser(roles = "SUPERVISOR_OPERATIVO")
    void getAllAlerts_SupervisorOperativo_Devuelve200() throws Exception {
        when(alertRepository.findByStatus(anyBoolean(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/alerts"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/alerts/refresh: SUPERVISOR_OPERATIVO puede refrescar alertas")
    @WithMockUser(roles = "SUPERVISOR_OPERATIVO")
    void refreshAlerts_SupervisorOperativo_Devuelve200() throws Exception {
        when(alertRepository.count()).thenReturn(0L);
        when(alertRepository.findByStatus(anyBoolean(), any())).thenReturn(Page.empty());

        mockMvc.perform(post("/api/v1/alerts/refresh").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/alerts/calculate: SUPERVISOR_OPERATIVO también puede (hace lo mismo que /refresh)")
    @WithMockUser(roles = "SUPERVISOR_OPERATIVO")
    void calculateAlerts_SupervisorOperativo_Devuelve200() throws Exception {
        when(alertRepository.count()).thenReturn(0L);
        when(alertRepository.findByStatus(anyBoolean(), any())).thenReturn(Page.empty());

        mockMvc.perform(post("/api/v1/alerts/calculate").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/alerts/calculate: ADMIN también puede")
    @WithMockUser(roles = "ADMIN")
    void calculateAlerts_Admin_Devuelve200() throws Exception {
        when(alertRepository.count()).thenReturn(0L);
        when(alertRepository.findByStatus(anyBoolean(), any())).thenReturn(Page.empty());

        mockMvc.perform(post("/api/v1/alerts/calculate").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/alerts/debug/oil-changes: SUPERVISOR_OPERATIVO NO puede (solo ADMIN, es diagnóstico)")
    @WithMockUser(roles = "SUPERVISOR_OPERATIVO")
    void debugOilChanges_SupervisorOperativo_Devuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/alerts/debug/oil-changes"))
                .andExpect(status().isForbidden());
    }
}
