package com.app.usochicamochabackend.machine.web;

import com.app.usochicamochabackend.auth.utils.JwtUtils;
import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.machine.application.dto.MachineRequest;
import com.app.usochicamochabackend.machine.application.dto.MachineResponse;
import com.app.usochicamochabackend.machine.application.port.*;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MachineController.class)
@AutoConfigureMockMvc
class MachineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateMachineUseCase createMachineUseCase;

    @MockBean
    private DeleteMachineUseCase deleteMachineUseCase;

    @MockBean
    private FindAllMachinesUseCase findAllMachinesUseCase;

    @MockBean
    private FindMachineByIdUseCase findMachineByIdUseCase;

    @MockBean
    private UpdateMachineUseCase updateMachineUseCase;

    @MockBean
    private JwtUtils jwtUtils;

    private static final MachineResponse MOCK_RESPONSE = new MachineResponse(
            1L, "Excavadora", "Usochicamocha", "2020", null, "Caterpillar",
            null, "ENG001", "VIN001", 0);

    private MachineRequest buildRequest() {
        return new MachineRequest("Excavadora", "Usochicamocha", "2020", null, "Caterpillar",
                null, "ENG001", "VIN001");
    }

    @Test
    @DisplayName("GET /api/v1/machine: retorna lista de máquinas")
    @WithMockUser
    void findAll_RetornaLista() throws Exception {
        when(findAllMachinesUseCase.findAllMachines()).thenReturn(List.of(MOCK_RESPONSE));

        mockMvc.perform(get("/api/v1/machine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Excavadora"));
    }

    @Test
    @DisplayName("GET /api/v1/machine/{id}: máquina existente → 200")
    @WithMockUser
    void getById_Existente_Devuelve200() throws Exception {
        when(findMachineByIdUseCase.findMachineById(1L)).thenReturn(MOCK_RESPONSE);

        mockMvc.perform(get("/api/v1/machine/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Excavadora"));
    }

    @Test
    @DisplayName("GET /api/v1/machine/{id}: máquina no encontrada → 404")
    @WithMockUser
    void getById_NoExiste_Devuelve404() throws Exception {
        when(findMachineByIdUseCase.findMachineById(99L))
                .thenThrow(new ResourceNotFoundException("Machine not found with ID: 99"));

        mockMvc.perform(get("/api/v1/machine/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/machine: crea máquina → 201")
    @WithMockUser(roles = "ADMIN")
    void create_DatosValidos_Devuelve201() throws Exception {
        when(createMachineUseCase.createMachine(any())).thenReturn(MOCK_RESPONSE);

        mockMvc.perform(post("/api/v1/machine")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Excavadora"));

    }

    @Test
    @DisplayName("PUT /api/v1/machine/{id}: actualiza máquina → 201")
    @WithMockUser(roles = "ADMIN")
    void update_DatosValidos_Devuelve201() throws Exception {
        when(updateMachineUseCase.updateMachine(any(), eq(1L))).thenReturn(MOCK_RESPONSE);

        mockMvc.perform(put("/api/v1/machine/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("DELETE /api/v1/machine/{id}: elimina máquina → 204")
    @WithMockUser(roles = "ADMIN")
    void delete_Existente_Devuelve204() throws Exception {
        doNothing().when(deleteMachineUseCase).deleteMachine(1L);

        mockMvc.perform(delete("/api/v1/machine/1").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
