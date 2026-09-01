package com.app.usochicamochabackend.fuel.web;

import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.auth.utils.JwtUtils;
import com.app.usochicamochabackend.fuel.application.dto.FuelMonthlyDiscountResponse;
import com.app.usochicamochabackend.fuel.application.port.ManageFuelMonthlyDiscountUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * La autorización real por rol (ADMIN/SUPERVISOR_OPERATIVO) vive en SecurityConfig,
 * no en @PreAuthorize (el proyecto no tiene @EnableMethodSecurity) — mismo patrón que
 * FuelPurchaseControllerTest. Ese slice no carga el filtro real, así que aquí solo se
 * prueba el comportamiento del controller con un principal ya autenticado.
 */
@WebMvcTest(FuelMonthlyDiscountController.class)
@AutoConfigureMockMvc
class FuelMonthlyDiscountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ManageFuelMonthlyDiscountUseCase manageFuelMonthlyDiscountUseCase;

    @MockBean
    private JwtUtils jwtUtils;

    private static RequestPostProcessor as(Long id, String username, String role) {
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
        Authentication auth = new UsernamePasswordAuthenticationToken(new UserPrincipal(id, username), null, List.of(authority));
        return authentication(auth);
    }

    @Test
    @DisplayName("POST /monthly-discount: SUPERVISOR_OPERATIVO autorizado, devuelve 201")
    void registrar_ConRolSupervisor_Devuelve201() throws Exception {
        FuelMonthlyDiscountResponse response = new FuelMonthlyDiscountResponse(
                1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), new BigDecimal("80000"));
        when(manageFuelMonthlyDiscountUseCase.registrar(any(), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/v1/fuel/monthly-discount")
                        .contentType("application/json")
                        .content("{\"fechaInicio\":\"2026-07-01\",\"fechaFin\":\"2026-07-31\",\"monto\":80000}")
                        .with(csrf())
                        .with(as(1L, "supervisor", "SUPERVISOR_OPERATIVO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /monthly-discount: ADMIN autorizado, devuelve 200")
    @WithMockUser(roles = "ADMIN")
    void listar_ConRolAdmin_Devuelve200() throws Exception {
        when(manageFuelMonthlyDiscountUseCase.listar()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/fuel/monthly-discount"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /monthly-discount: sin autenticar, devuelve 401")
    void listar_SinAutenticar_Devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/fuel/monthly-discount"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /monthly-discount/{id}: ADMIN autorizado, devuelve 204")
    @WithMockUser(roles = "ADMIN")
    void eliminar_ConRolAdmin_Devuelve204() throws Exception {
        mockMvc.perform(delete("/api/v1/fuel/monthly-discount/1").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
