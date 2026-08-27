package com.app.usochicamochabackend.fuel.web;

import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.auth.utils.JwtUtils;
import com.app.usochicamochabackend.fuel.application.dto.RefuelingRecordResponse;
import com.app.usochicamochabackend.fuel.application.port.RegisterRefuelingRecordUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpMethod;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RefuelingRecordController.class)
@AutoConfigureMockMvc
class RefuelingRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegisterRefuelingRecordUseCase registerRefuelingRecordUseCase;

    @MockBean
    private JwtUtils jwtUtils;

    private static RequestPostProcessor as(Long id, String username, String role) {
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
        Authentication auth = new UsernamePasswordAuthenticationToken(new UserPrincipal(id, username), null, List.of(authority));
        return authentication(auth);
    }

    @Test
    @DisplayName("POST /refueling: OPERARIO autorizado (tanqueo ALMACEN), devuelve 201")
    void registrar_ConRolOperario_Devuelve201() throws Exception {
        RefuelingRecordResponse response = new RefuelingRecordResponse(
                1L, null, 99L, "ALMACEN", "DISTRITO", 1L, new BigDecimal("30"), new BigDecimal("500"),
                false, null, null, null, null, false, null, "Bodega", 1L, LocalDateTime.now(), false, false, false, false, BigDecimal.ZERO,
                null, null, null, null, null, null);
        when(registerRefuelingRecordUseCase.registrar(any(), any(), anyLong())).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/fuel/refueling")
                        .part(new MockPart("machineId", "99".getBytes()))
                        .part(new MockPart("lugar", "ALMACEN".getBytes()))
                        .part(new MockPart("areaCosto", "DISTRITO".getBytes()))
                        .part(new MockPart("fuelTypeId", "1".getBytes()))
                        .part(new MockPart("cantidadGalones", "30".getBytes()))
                        .part(new MockPart("horometroKm", "500".getBytes()))
                        .with(csrf())
                        .with(as(3L, "operario", "OPERARIO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    // Nota: la matriz de permisos real (rol correcto vs incorrecto) se prueba en
    // FuelModuleE2ETest — ver comentario equivalente en FuelPurchaseControllerTest.

    @Test
    @DisplayName("GET /refueling: ALMACEN autorizado, devuelve 200")
    @WithMockUser(roles = "ALMACEN")
    void listar_ConRolAlmacen_Devuelve200() throws Exception {
        Page<RefuelingRecordResponse> page = new PageImpl<>(List.of());
        when(registerRefuelingRecordUseCase.listar(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/fuel/refueling"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /refueling/{id}: ADMIN autorizado, devuelve 200")
    void actualizar_ConRolAdmin_Devuelve200() throws Exception {
        RefuelingRecordResponse response = new RefuelingRecordResponse(
                1L, null, 99L, "ALMACEN", "DISTRITO", 1L, new BigDecimal("30"), new BigDecimal("500"),
                false, null, null, null, null, false, null, "Bodega", 1L, LocalDateTime.now(), false, false, false, false, BigDecimal.ZERO,
                null, null, null, null, null, null);
        when(registerRefuelingRecordUseCase.actualizar(anyLong(), any(), any())).thenReturn(response);

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/fuel/refueling/{id}", 1L)
                        .part(new MockPart("machineId", "99".getBytes()))
                        .part(new MockPart("lugar", "ALMACEN".getBytes()))
                        .part(new MockPart("areaCosto", "DISTRITO".getBytes()))
                        .part(new MockPart("fuelTypeId", "1".getBytes()))
                        .part(new MockPart("cantidadGalones", "30".getBytes()))
                        .part(new MockPart("horometroKm", "500".getBytes()))
                        .with(csrf())
                        .with(as(3L, "admin", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("DELETE /refueling/{id}: ADMIN autorizado, devuelve 204")
    void eliminar_ConRolAdmin_Devuelve204() throws Exception {
        doNothing().when(registerRefuelingRecordUseCase).eliminar(1L);

        mockMvc.perform(delete("/api/v1/fuel/refueling/{id}", 1L)
                        .with(csrf())
                        .with(as(3L, "admin", "ADMIN")))
                .andExpect(status().isNoContent());
    }

    // Nota: la matriz de permisos (rol distinto de ADMIN -> 403) se prueba en
    // FuelModuleE2ETest, mismo criterio que el resto de este archivo.
}
