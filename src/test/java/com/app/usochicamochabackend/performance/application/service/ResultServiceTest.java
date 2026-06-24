package com.app.usochicamochabackend.performance.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.auth.infrastructure.repository.UserRepositoryJpa;
import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.order.application.port.GetOrderByIdUseCase;
import com.app.usochicamochabackend.order.infrastructure.entity.OrderEntity;
import com.app.usochicamochabackend.order.infrastructure.repository.OrderRepository;
import com.app.usochicamochabackend.performance.application.dto.ExecuteAnOrderRequest;
import com.app.usochicamochabackend.performance.infrastructure.entity.ResultEntity;
import com.app.usochicamochabackend.performance.infrastructure.repository.ResultRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResultServiceTest {

    @Mock
    private ResultRepository resultRepository;

    @Mock
    private GetOrderByIdUseCase getOrderByIdUseCase;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepositoryJpa userRepository;

    @Mock
    private SaveActionUseCase saveActionUseCase;

    @InjectMocks
    private ResultService resultService;

    @BeforeEach
    void setupSecurityContext() {
        UserPrincipal principal = new UserPrincipal(1L, "tecnico");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("execute: orden no encontrada → lanza ResourceNotFoundException")
    void execute_OrdenNoEncontrada_LanzaExcepcion() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        ExecuteAnOrderRequest request = new ExecuteAnOrderRequest(99L, 2, 30, "Desc", null, null);
        assertThrows(ResourceNotFoundException.class, () -> resultService.execute(request));
    }

    @Test
    @DisplayName("execute: orden ya ejecutada → lanza RuntimeException")
    void execute_OrdenYaEjecutada_LanzaRuntimeException() {
        OrderEntity order = OrderEntity.builder()
                .id(1L).status("Completada").result(new ResultEntity()).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        ExecuteAnOrderRequest request = new ExecuteAnOrderRequest(1L, 1, 0, "Desc", null, null);
        assertThrows(RuntimeException.class, () -> resultService.execute(request));
    }

    @Test
    @DisplayName("execute: orden válida → guarda resultado y actualiza estado a 'Completada'")
    void execute_OrdenValida_GuardaResultado() {
        OrderEntity order = OrderEntity.builder().id(2L).status("Pendiente").build();
        ResultEntity savedResult = new ResultEntity();
        savedResult.setId(10L);

        when(orderRepository.findById(2L)).thenReturn(Optional.of(order));
        when(resultRepository.save(any())).thenReturn(savedResult);
        when(orderRepository.save(any())).thenReturn(order);
        when(getOrderByIdUseCase.getOrderById(2L)).thenReturn(null);

        ExecuteAnOrderRequest request = new ExecuteAnOrderRequest(2L, 1, 30, "Trabajo completado", null, null);
        resultService.execute(request);

        assertEquals("Completada", order.getStatus());
        verify(resultRepository).save(any(ResultEntity.class));
        verify(resultRepository).flush();
        verify(saveActionUseCase).save(anyString());
    }
}
