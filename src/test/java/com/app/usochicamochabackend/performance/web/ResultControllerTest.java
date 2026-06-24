package com.app.usochicamochabackend.performance.web;

import com.app.usochicamochabackend.auth.utils.JwtUtils;
import com.app.usochicamochabackend.order.application.dto.OrderResponse;
import com.app.usochicamochabackend.performance.application.dto.ExecuteAnOrderRequest;
import com.app.usochicamochabackend.performance.application.dto.ExecuteDTO;
import com.app.usochicamochabackend.performance.application.port.ExecuteAnOrderUseCase;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ResultController.class)
@AutoConfigureMockMvc
class ResultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExecuteAnOrderUseCase executeAnOrderUseCase;

    @MockBean
    private JwtUtils jwtUtils;

    @Test
    @DisplayName("POST /api/v1/result/execute: ejecuta orden → 200")
    @WithMockUser(roles = "ADMIN")
    void executeOrder_DatosValidos_Devuelve200() throws Exception {
        ExecuteDTO executeDTO = new ExecuteDTO(null, null, "Trabajo completado", "2h 30m", null, null);
        when(executeAnOrderUseCase.execute(any(ExecuteAnOrderRequest.class))).thenReturn(executeDTO);

        ExecuteAnOrderRequest request = new ExecuteAnOrderRequest(1L, 2, 30, "Trabajo completado", null, null);

        mockMvc.perform(post("/api/v1/results/execute")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/result/execute: orden ya ejecutada → 500")
    @WithMockUser(roles = "ADMIN")
    void executeOrder_OrdenYaEjecutada_Devuelve500() throws Exception {
        when(executeAnOrderUseCase.execute(any(ExecuteAnOrderRequest.class)))
                .thenThrow(new RuntimeException("Order has already been executed"));

        ExecuteAnOrderRequest request = new ExecuteAnOrderRequest(1L, 1, 0, "desc", null, null);

        mockMvc.perform(post("/api/v1/results/execute")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }
}
