package com.app.usochicamochabackend.vehicle.web;

import com.app.usochicamochabackend.auth.utils.JwtUtils;
import com.app.usochicamochabackend.vehicle.application.dto.VehicleRequest;
import com.app.usochicamochabackend.vehicle.application.dto.VehicleResponse;
import com.app.usochicamochabackend.vehicle.application.service.VehicleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VehicleController.class)
@AutoConfigureMockMvc
class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VehicleService vehicleService;

    @MockBean
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    private static final VehicleResponse RESPONSE_MOCK = new VehicleResponse(
            1, "ABC123", "Toyota", 1, 2, "AUTOMOVIL", 50000, "Distrito", null, null, true, null, null, null);

    private static final VehicleRequest REQUEST_VALIDO = new VehicleRequest(
            "ABC123", 1, 2, 50000, "Distrito", null, true);

    // --- GET /api/v1/vehicle ---

    @Test
    @WithMockUser
    void getAllVehicles_DebeRetornar200ConLista() throws Exception {
        when(vehicleService.findAllVehicles()).thenReturn(List.of(RESPONSE_MOCK));

        mockMvc.perform(get("/api/v1/vehicle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].placa").value("ABC123"))
                .andExpect(jsonPath("$[0].tipoVehiculo").value("AUTOMOVIL"));
    }

    // --- GET /api/v1/vehicle/{placa} ---

    @Test
    @WithMockUser
    void getByPlaca_CuandoExiste_DebeRetornar200() throws Exception {
        when(vehicleService.findByPlaca("ABC123")).thenReturn(RESPONSE_MOCK);

        mockMvc.perform(get("/api/v1/vehicle/ABC123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placa").value("ABC123"));
    }

    @Test
    @WithMockUser
    void getByPlaca_CuandoNoExiste_DebeRetornar404() throws Exception {
        when(vehicleService.findByPlaca("NOEXISTE"))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "Vehículo no encontrado con placa: NOEXISTE"));

        mockMvc.perform(get("/api/v1/vehicle/NOEXISTE"))
                .andExpect(status().isNotFound());
    }

    // --- POST /api/v1/vehicle ---

    @Test
    @WithMockUser
    void createVehicle_ConDatosValidos_DebeRetornar201() throws Exception {
        when(vehicleService.createVehicle(any())).thenReturn(RESPONSE_MOCK);

        mockMvc.perform(post("/api/v1/vehicle")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(REQUEST_VALIDO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.placa").value("ABC123"));
    }

    @Test
    @WithMockUser
    void createVehicle_PlacaDuplicada_DebeRetornar400() throws Exception {
        when(vehicleService.createVehicle(any()))
                .thenThrow(new ResponseStatusException(BAD_REQUEST, "Ya existe un vehículo con esta placa"));

        mockMvc.perform(post("/api/v1/vehicle")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(REQUEST_VALIDO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void createVehicle_KilometrajeNegativo_DebeRetornar400() throws Exception {
        VehicleRequest reqInvalido = new VehicleRequest("ABC123", 1, 2, -100, "Distrito", null, true);

        mockMvc.perform(post("/api/v1/vehicle")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqInvalido)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void createVehicle_SinPlaca_DebeRetornar400() throws Exception {
        VehicleRequest reqSinPlaca = new VehicleRequest("", 1, 2, 50000, "Distrito", null, true);

        mockMvc.perform(post("/api/v1/vehicle")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqSinPlaca)))
                .andExpect(status().isBadRequest());
    }

    // --- PUT /api/v1/vehicle/{id} ---

    @Test
    @WithMockUser
    void updateVehicle_ConDatosValidos_DebeRetornar200() throws Exception {
        when(vehicleService.updateVehicle(eq(1), any())).thenReturn(RESPONSE_MOCK);

        mockMvc.perform(put("/api/v1/vehicle/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(REQUEST_VALIDO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placa").value("ABC123"));
    }

    @Test
    @WithMockUser
    void updateVehicle_CuandoNoExiste_DebeRetornar404() throws Exception {
        when(vehicleService.updateVehicle(eq(99), any()))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "Vehículo no encontrado"));

        mockMvc.perform(put("/api/v1/vehicle/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(REQUEST_VALIDO)))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /api/v1/vehicle/{id} ---

    @Test
    @WithMockUser
    void deleteVehicle_CuandoExiste_DebeRetornar204() throws Exception {
        doNothing().when(vehicleService).deleteVehicle(1);

        mockMvc.perform(delete("/api/v1/vehicle/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void deleteVehicle_CuandoNoExiste_DebeRetornar404() throws Exception {
        doThrow(new ResponseStatusException(NOT_FOUND, "Vehículo no encontrado"))
                .when(vehicleService).deleteVehicle(99);

        mockMvc.perform(delete("/api/v1/vehicle/99").with(csrf()))
                .andExpect(status().isNotFound());
    }
}
