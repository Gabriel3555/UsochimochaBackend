package com.app.usochicamochabackend.mapper;

import com.app.usochicamochabackend.auth.infrastructure.entity.UserEntity;
import com.app.usochicamochabackend.auth.infrastructure.repository.UserRepositoryJpa;
import com.app.usochicamochabackend.order.application.dto.OrderResponse;
import com.app.usochicamochabackend.order.infrastructure.entity.OrderEntity;
import com.app.usochicamochabackend.order.infrastructure.repository.OrderRepository;
import com.app.usochicamochabackend.performance.application.dto.*;
import com.app.usochicamochabackend.performance.infrastructure.entity.LaborEntity;
import com.app.usochicamochabackend.performance.infrastructure.entity.ResultEntity;
import com.app.usochicamochabackend.performance.infrastructure.entity.SparePartEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ResultMapper {

    private ResultMapper() {}

    /**
     * Valida que los tiempos de ejecución sean válidos
     * - Horas >= 0
     * - Minutos entre 0-59
     */
    private static void validateExecutionTime(Integer hours, Integer minutes) {
        if (hours == null || hours < 0) {
            throw new IllegalArgumentException("Las horas deben ser mayor o igual a 0");
        }
        if (minutes == null || minutes < 0 || minutes > 59) {
            throw new IllegalArgumentException("Los minutos deben estar entre 0 y 59");
        }
    }

    public static ResultEntity toEntity(
            ExecuteAnOrderRequest request,
            OrderEntity order,
            UserRepositoryJpa userRepository
    ) {
        if (request == null || order == null) return null;

        // Validar tiempos
        validateExecutionTime(request.hoursSpent(), request.minutesSpent());

        ResultEntity result = new ResultEntity();
        result.setDate(LocalDateTime.now());
        result.setHoursSpent(request.hoursSpent());
        result.setMinutesSpent(request.minutesSpent());
        result.setDescription(request.description());

        UserEntity mechanic = null;
        if (request.labor() != null && Boolean.TRUE.equals(request.labor().sameMecanic())) {
            if (order.getInspection() != null) {
                mechanic = order.getInspection().getUser();
            } else if (order.getVehicleInspection() != null) {
                String loginUser = order.getVehicleInspection().getLoginUser();
                if (loginUser != null) {
                    mechanic = userRepository.findByUsername(loginUser).orElse(null);
                }
            }
        }

        LaborEntity labor = LaborMapper.toEntity(request.labor(), mechanic);
        result.setLaborForce(labor);

        SparePartEntity spareParts = SparePartMapper.toEntity(request.sparePart());

        result.setSparePart(spareParts);

        return result;
    }

    public static ExecuteDTO toResponse(ResultEntity entity, OrderResponse orderResponse) {
        if (entity == null) return null;

        SparePartResponse sparePart = SparePartMapper.toResponse(entity.getSparePart());

        LaborResponse labor = LaborMapper.toResponse(entity.getLaborForce());

        return new ExecuteDTO(
                orderResponse,
                entity.getDate(),
                entity.getDescription(),
                entity.getTimeSpent(),
                labor,
                sparePart
        );
    }

    public static ResultDTO toResponseResult(ResultEntity entity) {
        if (entity == null) return null;

        SparePartResponse sparePart = SparePartMapper.toResponse(entity.getSparePart());

        LaborResponse labor = LaborMapper.toResponse(entity.getLaborForce());

        OrderResponse order = OrderMapper.toDto(entity.getOrder());

        Double hourMeter = order != null && order.inspection() != null ? order.inspection().hourMeter() : null;

        BigDecimal laborPrice = labor != null ? labor.price() : BigDecimal.ZERO;
        BigDecimal sparePrice = sparePart != null ? sparePart.price() : BigDecimal.ZERO;

        String timeSpent = formatTimeSpent(entity.getHoursSpent(), entity.getMinutesSpent());

        return new ResultDTO(
                entity.getId(),
                entity.getDate(),
                entity.getDescription(),
                hourMeter,
                timeSpent,
                labor,
                sparePart,
                laborPrice.add(sparePrice)
        );
    }

    private static String formatTimeSpent(Integer hours, Integer minutes) {
        if (hours == null && minutes == null) return null;

        int h = hours != null ? hours : 0;
        int m = minutes != null ? minutes : 0;

        if (h == 0 && m == 0) return null;
        if (h == 0) return m + "m";
        if (m == 0) return h + "h";
        return h + "h " + m + "m";
    }

    public static List<ResultDTO> toResponseList(List<ResultEntity> entityList) {
        if (entityList == null) return null;
        return entityList.stream().map(ResultMapper::toResponseResult).toList();
    }
}
