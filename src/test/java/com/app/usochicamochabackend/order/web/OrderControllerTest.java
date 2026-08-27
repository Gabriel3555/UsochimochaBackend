package com.app.usochicamochabackend.order.web;

import com.app.usochicamochabackend.auth.utils.JwtUtils;
import com.app.usochicamochabackend.order.application.dto.*;
import com.app.usochicamochabackend.order.application.port.*;
import com.app.usochicamochabackend.update.application.service.ExcelGenerationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AssignOrderUseCase assignOrderUseCase;

    @MockBean
    private GetAllOrdersUseCase getAllOrdersUseCase;

    @MockBean
    private AssignVehicleOrderUseCase assignVehicleOrderUseCase;

    @MockBean
    private GetAllVehicleOrdersUseCase getAllVehicleOrdersUseCase;

    @MockBean
    private ExcelGenerationService excelGenerationService;

    @MockBean
    private JwtUtils jwtUtils;

    private OrderResponse buildOrderResponse() {
        return new OrderResponse(1L, "Pendiente", LocalDateTime.now(), "Descripción",
                null, null, "MAQUINARIA", "CORRECTIVO", "MOTOR", "OT-MQ-00001");
    }

    private OrderWithoutInspectionResponse buildOrderWithoutInspection() {
        return new OrderWithoutInspectionResponse(1L, "Pendiente", LocalDateTime.now(), "Desc",
                null, "MAQUINARIA", "CORRECTIVO", "MOTOR", "OT-MQ-00001", null, null, null, null);
    }

    @Test
    @DisplayName("POST /api/v1/order: asigna orden → 200")
    @WithMockUser(roles = "ADMIN")
    void assignOrder_DatosValidos_Devuelve200() throws Exception {
        when(assignOrderUseCase.assignOrder(any(AssignOrderRequest.class))).thenReturn(buildOrderResponse());

        AssignOrderRequest request = new AssignOrderRequest(1L, "Descripción", "MAQUINARIA", "CORRECTIVO", "MOTOR");

        mockMvc.perform(post("/api/v1/order")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("Pendiente"))
                .andExpect(jsonPath("$.consecutive").value("OT-MQ-00001"));    }

    @Test
    @DisplayName("GET /api/v1/order/all: retorna página de órdenes con máquina")
    @WithMockUser
    void getAllOrders_RetornaPagina() throws Exception {
        OrderWithMachineDTO dto = new OrderWithMachineDTO(buildOrderWithoutInspection(), null);
        when(getAllOrdersUseCase.getAllOrders(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/v1/order/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/order/vehicle: asigna orden de vehículo → 201")
    @WithMockUser(roles = "ADMIN")
    void assignVehicleOrder_Devuelve201() throws Exception {
        OrderWithVehicleDTO dto = new OrderWithVehicleDTO(buildOrderWithoutInspection(), null);
        when(assignVehicleOrderUseCase.assignVehicleOrder(any(AssignVehicleOrderRequest.class))).thenReturn(dto);

        AssignVehicleOrderRequest request = new AssignVehicleOrderRequest(1L, "Desc", "VEHICULO", "PREVENTIVO", "FRENOS");

        mockMvc.perform(post("/api/v1/order/vehicle")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET /api/v1/order/vehicle/all: retorna página de órdenes de vehículos")
    @WithMockUser
    void getAllVehicleOrders_RetornaPagina() throws Exception {
        OrderWithVehicleDTO dto = new OrderWithVehicleDTO(buildOrderWithoutInspection(), null);
        when(getAllVehicleOrdersUseCase.getAllVehicleOrders(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/v1/order/vehicle/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }
}
