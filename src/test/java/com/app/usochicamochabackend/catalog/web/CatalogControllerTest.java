package com.app.usochicamochabackend.catalog.web;

import com.app.usochicamochabackend.auth.utils.JwtUtils;
import com.app.usochicamochabackend.catalog.application.dto.CatalogDTO;
import com.app.usochicamochabackend.catalog.application.service.AreaService;
import com.app.usochicamochabackend.catalog.application.service.TipoVehiculoService;
import com.app.usochicamochabackend.catalog.application.service.UbicacionService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CatalogController.class)
@AutoConfigureMockMvc
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AreaService areaService;

    @MockBean
    private UbicacionService ubicacionService;

    @MockBean
    private TipoVehiculoService tipoVehiculoService;

    @MockBean
    private JwtUtils jwtUtils;

    @Test
    @DisplayName("GET /api/v1/catalog/area: retorna lista de áreas")
    @WithMockUser(roles = "ADMIN")
    void getAreas_RetornaLista() throws Exception {
        when(areaService.findAll()).thenReturn(List.of(
                new CatalogDTO(1, "Mecánica", true),
                new CatalogDTO(2, "Sistemas", true)));

        mockMvc.perform(get("/api/v1/catalog/area"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Mecánica"));
    }

    @Test
    @DisplayName("POST /api/v1/catalog/area: crea área y devuelve 200")
    @WithMockUser(roles = "ADMIN")
    void createArea_DatosValidos_Devuelve200() throws Exception {
        CatalogDTO request = new CatalogDTO(null, "Nuevas obras", true);
        CatalogDTO response = new CatalogDTO(5, "Nuevas Obras", true);
        when(areaService.create(any(CatalogDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/catalog/area")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Nuevas Obras"));
    }

    @Test
    @DisplayName("PUT /api/v1/catalog/area/{id}: actualiza área existente")
    @WithMockUser(roles = "ADMIN")
    void updateArea_DatosValidos_Devuelve200() throws Exception {
        CatalogDTO request = new CatalogDTO(null, "Mecánica Avanzada", true);
        CatalogDTO response = new CatalogDTO(1, "Mecánica Avanzada", true);
        when(areaService.update(eq(1), any(CatalogDTO.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/catalog/area/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mecánica Avanzada"));
    }

    @Test
    @DisplayName("GET /api/v1/catalog/ubicacion: retorna lista de ubicaciones")
    @WithMockUser(roles = "ADMIN")
    void getUbicaciones_RetornaLista() throws Exception {
        when(ubicacionService.findAll()).thenReturn(List.of(
                new CatalogDTO(1, "Bodega Central", true)));

        mockMvc.perform(get("/api/v1/catalog/ubicacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Bodega Central"));
    }

    @Test
    @DisplayName("GET /api/v1/catalog/tipo-vehiculo: retorna lista de tipos")
    @WithMockUser(roles = "ADMIN")
    void getTiposVehiculo_RetornaLista() throws Exception {
        when(tipoVehiculoService.findAll()).thenReturn(List.of(
                new CatalogDTO(1, "AUTOMOVIL", true),
                new CatalogDTO(2, "MOTOCICLETA", true)));

        mockMvc.perform(get("/api/v1/catalog/tipo-vehiculo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
