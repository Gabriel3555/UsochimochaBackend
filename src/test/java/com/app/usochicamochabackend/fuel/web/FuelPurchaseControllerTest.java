package com.app.usochicamochabackend.fuel.web;

import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.auth.utils.JwtUtils;
import com.app.usochicamochabackend.fuel.application.dto.FuelPurchaseResponse;
import com.app.usochicamochabackend.fuel.application.port.RegisterFuelPurchaseUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Los endpoints POST leen el principal como UserPrincipal (no el User genérico de Spring
 * Security), así que las pruebas de escritura necesitan una autenticación real con ese tipo
 * en vez de @WithMockUser — igual que en FuelModuleE2ETest.
 */
@WebMvcTest(FuelPurchaseController.class)
@AutoConfigureMockMvc
class FuelPurchaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegisterFuelPurchaseUseCase registerFuelPurchaseUseCase;

    @MockBean
    private JwtUtils jwtUtils;

    private final MockMultipartFile factura = new MockMultipartFile("factura", "f.pdf", "application/pdf", new byte[]{1, 2, 3});

    private static RequestPostProcessor as(Long id, String username, String role) {
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
        Authentication auth = new UsernamePasswordAuthenticationToken(new UserPrincipal(id, username), null, List.of(authority));
        return authentication(auth);
    }

    // Nota: la autorización por rol para POST/PUT/DELETE la hace SecurityConfig
    // (requestMatchers + hasAnyRole), no @PreAuthorize — el proyecto no tiene
    // @EnableMethodSecurity, así que esas anotaciones no se evalúan realmente.
    // Un @WebMvcTest en slice no carga ese filtro, por eso la matriz de permisos
    // real (rol correcto vs incorrecto) se prueba en FuelModuleE2ETest, no aquí.

    @Test
    @DisplayName("POST /purchases: SUPERVISOR_OPERATIVO autorizado, devuelve 201")
    void registrar_ConRolSupervisor_Devuelve201() throws Exception {
        FuelPurchaseResponse response = new FuelPurchaseResponse(
                1L, "DISTRITO", 1L, new BigDecimal("100"), new BigDecimal("10000"), BigDecimal.ZERO,
                new BigDecimal("1000000"), new BigDecimal("1000000"), false,
                "/uploads/documents/fuel/purchases/1/current.pdf", 1L, LocalDateTime.now());
        when(registerFuelPurchaseUseCase.registrar(any(), any(), anyLong())).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/fuel/purchases")
                        .file(factura)
                        .part(new MockPart("areaCosto", "DISTRITO".getBytes()))
                        .part(new MockPart("fuelTypeId", "1".getBytes()))
                        .part(new MockPart("cantidad", "100".getBytes()))
                        .part(new MockPart("precioUnitario", "10000".getBytes()))
                        .part(new MockPart("totalIngresado", "1000000".getBytes()))
                        .with(csrf())
                        .with(as(1L, "supervisor", "SUPERVISOR_OPERATIVO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /purchases: ALMACEN autorizado, devuelve 200")
    @WithMockUser(roles = "ALMACEN")
    void listar_ConRolAlmacen_Devuelve200() throws Exception {
        Page<FuelPurchaseResponse> page = new PageImpl<>(List.of());
        when(registerFuelPurchaseUseCase.listar(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/fuel/purchases"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /purchases: sin autenticar, devuelve 401")
    void listar_SinAutenticar_Devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/fuel/purchases"))
                .andExpect(status().isUnauthorized());
    }
}
