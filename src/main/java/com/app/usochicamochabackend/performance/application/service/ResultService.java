package com.app.usochicamochabackend.performance.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.auth.infrastructure.repository.UserRepositoryJpa;
import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.mapper.ResultMapper;
import com.app.usochicamochabackend.order.application.dto.OrderResponse;
import com.app.usochicamochabackend.order.application.port.GetOrderByIdUseCase;
import com.app.usochicamochabackend.order.infrastructure.entity.OrderEntity;
import com.app.usochicamochabackend.order.infrastructure.repository.OrderRepository;
import com.app.usochicamochabackend.performance.application.dto.ExecuteAnOrderRequest;
import com.app.usochicamochabackend.performance.application.dto.ExecuteDTO;
import com.app.usochicamochabackend.performance.application.port.*;
import com.app.usochicamochabackend.performance.infrastructure.entity.ResultEntity;
import com.app.usochicamochabackend.performance.infrastructure.repository.ResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResultService implements ExecuteAnOrderUseCase {

    private final ResultRepository resultRepository;
    private final GetOrderByIdUseCase getOrderByIdUseCase;
    private final OrderRepository orderRepository;
    private final UserRepositoryJpa  userRepository;
    private final SaveActionUseCase saveActionUseCase;

    @Override
    @Transactional
    public ExecuteDTO execute(ExecuteAnOrderRequest request) {
        // Obtener la orden
        OrderEntity orderEntity = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (orderEntity.getResult() != null) {
            throw new RuntimeException("Order has already been executed");
        }

        // Crear y guardar el resultado PRIMERO
        ResultEntity result = ResultMapper.toEntity(request, orderEntity, userRepository);
        ResultEntity savedResult = resultRepository.save(result);
        resultRepository.flush(); // Forzar flush para obtener el ID

        // Asignar resultado a la orden
        orderEntity.setResult(savedResult);
        orderEntity.setStatus("Completada");

        // Guardar SOLO la orden (Hibernate manejará el resto)
        orderRepository.save(orderEntity);
        orderRepository.flush(); // Forzar flush para asegurar persistencia

        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var inspection = orderEntity.getInspection();
        var vehicleInspection = orderEntity.getVehicleInspection();
        String actionDetails;
        if (inspection != null && inspection.getMachine() != null) {
            actionDetails = "El usuario " + userPrincipal.username() + " ha ejecutado una orden de trabajo asignada a la inspeccion realizada a la maquina " + inspection.getMachine().getName() + " el dia " + inspection.getDateStamp();
        } else if (inspection != null) {
            actionDetails = "El usuario " + userPrincipal.username() + " ha ejecutado una orden de trabajo asignada a una inspección de máquina el día " + inspection.getDateStamp();
        } else if (vehicleInspection != null) {
            actionDetails = "El usuario " + userPrincipal.username() + " ha ejecutado una orden de trabajo del vehiculo con inspeccion " + vehicleInspection.getIdInspeccion() + " del dia " + vehicleInspection.getFechaRegistro();
        } else {
            actionDetails = "El usuario " + userPrincipal.username() + " ha ejecutado una orden de trabajo";
        }
        saveActionUseCase.save(actionDetails);

        OrderResponse orderResponse = getOrderByIdUseCase.getOrderById(request.orderId());
        return ResultMapper.toResponse(savedResult, orderResponse);
    }

}