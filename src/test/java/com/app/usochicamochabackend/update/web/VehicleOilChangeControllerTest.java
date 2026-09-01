package com.app.usochicamochabackend.update.web;

import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.auth.utils.JwtUtils;
import com.app.usochicamochabackend.update.application.service.VehicleOilChangeService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VehicleOilChangeController.class)
@AutoConfigureMockMvc
class VehicleOilChangeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VehicleOilChangeService vehicleOilChangeService;

    @MockBean
    private JwtUtils jwtUtils;

    private static RequestPostProcessor as(Long id, String username, String role) {
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
        Authentication auth = new UsernamePasswordAuthenticationToken(new UserPrincipal(id, username), null, List.of(authority));
        return authentication(auth);
    }

    private static final String BODY = """
            {"placa":"ABC123","dateStamp":"2026-08-01T10:00:00","oilType":"MOTOR","brandId":2,
             "quantity":4.0,"kmAtChange":15000,"intervalKm":5000,"airFilterChanged":true}""";

    // Nota: la matriz de permisos real (rol correcto vs incorrecto) se prueba en
    // un test end-to-end con contexto completo — @PreAuthorize no se aplica en el
    // slice @WebMvcTest (mismo criterio ya usado en RefuelingRecordControllerTest).

    @Test
    @DisplayName("PUT /vehicle/oil-change/{id}: devuelve 204")
    void actualizar_Devuelve204() throws Exception {
        doNothing().when(vehicleOilChangeService).actualizar(anyLong(), any());

        mockMvc.perform(put("/api/v1/vehicle/oil-change/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY)
                        .with(csrf())
                        .with(as(3L, "admin", "ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /vehicle/oil-change/{id}: devuelve 204")
    void eliminar_Devuelve204() throws Exception {
        doNothing().when(vehicleOilChangeService).eliminar(1L);

        mockMvc.perform(delete("/api/v1/vehicle/oil-change/{id}", 1L)
                        .with(csrf())
                        .with(as(3L, "admin", "ADMIN")))
                .andExpect(status().isNoContent());
    }
}
