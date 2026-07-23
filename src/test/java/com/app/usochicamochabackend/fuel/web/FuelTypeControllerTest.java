package com.app.usochicamochabackend.fuel.web;

import com.app.usochicamochabackend.auth.utils.JwtUtils;
import com.app.usochicamochabackend.fuel.infrastructure.entity.FuelTypesEntity;
import com.app.usochicamochabackend.fuel.infrastructure.repository.FuelTypesRepository;
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

@WebMvcTest(FuelTypeController.class)
@AutoConfigureMockMvc
class FuelTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FuelTypesRepository fuelTypesRepository;

    @MockBean
    private JwtUtils jwtUtils;

    @Test
    @DisplayName("GET /api/v1/fuel/types: usuario autenticado obtiene 200 con el catálogo")
    @WithMockUser(roles = "OPERARIO")
    void getTypes_ConUsuarioAutenticado_Devuelve200ConCuatroElementos() throws Exception {
        when(fuelTypesRepository.findAll()).thenReturn(List.of(
                FuelTypesEntity.builder().id(1L).codigo("GASOLINA_CORRIENTE").nombre("Gasolina corriente").activo(true).unidadMedida("GALON").build(),
                FuelTypesEntity.builder().id(2L).codigo("GASOLINA_EXTRA").nombre("Gasolina extra").activo(true).unidadMedida("GALON").build(),
                FuelTypesEntity.builder().id(3L).codigo("ACPM").nombre("ACPM / Diésel").activo(true).unidadMedida("GALON").build(),
                FuelTypesEntity.builder().id(4L).codigo("GAS").nombre("Gas natural vehicular").activo(true).unidadMedida("M3").build()));

        mockMvc.perform(get("/api/v1/fuel/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[2].codigo").value("ACPM"))
                .andExpect(jsonPath("$[3].unidadMedida").value("M3"));
    }

    @Test
    @DisplayName("GET /api/v1/fuel/types: sin autenticar devuelve 401")
    void getTypes_SinAutenticar_Devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/fuel/types"))
                .andExpect(status().isUnauthorized());
    }
}
