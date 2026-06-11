package com.app.usochicamochabackend.mapper;

import com.app.usochicamochabackend.order.application.dto.*;
import com.app.usochicamochabackend.order.infrastructure.entity.OrderEntity;
import com.app.usochicamochabackend.review.application.dto.InspectionFormResponse;
import com.app.usochicamochabackend.review.infrastructure.entity.InspectionEntity;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.entity.InspPreOperativaEntity;
import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;

import java.util.List;

public class OrderMapper {

    private OrderMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static OrderResponse toDto(OrderEntity entity) {
        if (entity == null) {
            return null;
        }

        InspectionEntity inspection = entity.getInspection();
        InspectionFormResponse inspectionDto = inspection == null ? null : InspectionMapper.toDto(inspection);

        return new OrderResponse(
                entity.getId(),
                entity.getStatus(),
                entity.getDate(),
                entity.getDescription(),
                inspectionDto,
                UserMapper.toResponse(entity.getAssignerUser()),
                entity.getOrderType(),
                entity.getMaintenanceType() != null ? entity.getMaintenanceType().name() : null,
                entity.getConsecutive()
        );
    }

    public static OrderWithoutInspectionResponse toDtoWithoutInspection(OrderEntity entity) {
        if (entity == null) {
            return null;
        }

        Integer hoursSpent = null;
        Integer minutesSpent = null;
        String suppliers = null;
        if (entity.getResult() != null) {
            hoursSpent = entity.getResult().getHoursSpent();
            minutesSpent = entity.getResult().getMinutesSpent();
            if (entity.getResult().getSparePart() != null) {
                suppliers = entity.getResult().getSparePart().getSupplier();
            }
        }

        return new OrderWithoutInspectionResponse(
                entity.getId(),
                entity.getStatus(),
                entity.getDate(),
                entity.getDescription(),
                UserMapper.toResponse(entity.getAssignerUser()),
                entity.getOrderType(),
                entity.getMaintenanceType() != null ? entity.getMaintenanceType().name() : null,
                entity.getConsecutive(),
                hoursSpent,
                minutesSpent,
                suppliers
        );
    }

    public static List<OrderWithoutInspectionResponse> toDtoListWithoutInspection(List<OrderEntity> entity) {
        if (entity == null) return null;

        return entity.stream().map(OrderMapper::toDtoWithoutInspection).toList();
    }

    public static OrderWithVehicleDTO toVehicleOrderDTO(OrderEntity entity) {
        if (entity == null) return null;

        OrderWithoutInspectionResponse orderDTO = toDtoWithoutInspection(entity);

        InspPreOperativaEntity vi = entity.getVehicleInspection();
        VehicleEntity vehiculo = vi != null ? vi.getVehiculo() : null;
        String placa = vehiculo != null ? vehiculo.getPlaca() : null;
        String marca = (vehiculo != null && vehiculo.getMarca() != null) ? vehiculo.getMarca().getDescripcion() : null;

        String tipoVehiculo = (vehiculo != null && vehiculo.getTipoVehiculo() != null)
                ? vehiculo.getTipoVehiculo().getNombreTipo() : null;
        Integer vehiculoId = vehiculo != null ? vehiculo.getIdVehiculo() : null;

        VehicleOrderSummaryDTO vehicleSummary = new VehicleOrderSummaryDTO(
                vi != null ? vi.getIdInspeccion() : null,
                placa,
                marca,
                vi != null ? vi.getFechaRegistro() : null,
                tipoVehiculo,
                vehiculoId
        );

        return new OrderWithVehicleDTO(orderDTO, vehicleSummary);
    }
}
