package com.app.usochicamochabackend.order.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.auth.infrastructure.entity.UserEntity;
import com.app.usochicamochabackend.auth.infrastructure.repository.UserRepositoryJpa;
import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.notifications.application.NotificationService;
import com.app.usochicamochabackend.order.application.dto.OrderWithoutInspectionResponse;
import com.app.usochicamochabackend.order.infrastructure.entity.OrderEntity;
import com.app.usochicamochabackend.order.infrastructure.repository.OrderCounterRepository;
import com.app.usochicamochabackend.order.infrastructure.repository.OrderRepository;
import com.app.usochicamochabackend.review.infrastructure.repository.InspectionRepository;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.repository.InspPreOperativaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderCounterRepository orderCounterRepository;

    @Mock
    private InspectionRepository inspectionRepository;

    @Mock
    private InspPreOperativaRepository inspPreOperativaRepository;

    @Mock
    private UserRepositoryJpa userRepository;

    @Mock
    private SaveActionUseCase saveActionUseCase;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setupSecurityContext() {
        UserPrincipal principal = new UserPrincipal(1L, "admin");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getOrderById: orden existente → retorna respuesta")
    void getOrderById_Existente_RetornaRespuesta() {
        OrderEntity entity = OrderEntity.builder()
                .id(1L).status("Pending").consecutive("OT-MQ-00001").build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(entity));

        var result = orderService.getOrderById(1L);

        assertNotNull(result);
        assertEquals("Pending", result.status());
    }

    @Test
    @DisplayName("getOrderById: orden no encontrada → lanza ResourceNotFoundException")
    void getOrderById_NoExiste_LanzaExcepcion() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderById(99L));
    }

    @Test
    @DisplayName("getAllOrders: retorna página de órdenes con máquina")
    void getAllOrders_RetornaPagina() {
        OrderEntity entity = OrderEntity.builder()
                .id(1L).status("Pending").consecutive("OT-MQ-00001").build();
        when(orderRepository.findAllByInspectionIsNotNull(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        var result = orderService.getAllOrders(PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("getAllVehicleOrders: retorna página de órdenes de vehículos")
    void getAllVehicleOrders_RetornaPagina() {
        OrderEntity entity = OrderEntity.builder()
                .id(2L).status("Completada").consecutive("OT-VH-00001").build();
        when(orderRepository.findAllByVehicleInspectionIsNotNull(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        var result = orderService.getAllVehicleOrders(PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}
