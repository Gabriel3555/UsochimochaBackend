package com.app.usochicamochabackend.mapper;

import com.app.usochicamochabackend.performance.application.dto.SparePartRequest;
import com.app.usochicamochabackend.performance.application.dto.SparePartResponse;
import com.app.usochicamochabackend.performance.infrastructure.entity.SparePartEntity;

public class SparePartMapper {

    private SparePartMapper() {}

    public static SparePartEntity toEntity(SparePartRequest request) {
        if (request == null) return null;

        SparePartEntity entity = new SparePartEntity();
        entity.setRef(request.ref());
        entity.setName(request.name());
        entity.setQuantity(request.quantity());
        entity.setPrice(request.price());
        entity.setSupplier(request.supplier());

        return entity;
    }

    public static SparePartResponse toResponse(SparePartEntity entity) {
        if (entity == null) return null;

        return new SparePartResponse(
                entity.getId(),
                entity.getRef(),
                entity.getName(),
                entity.getQuantity(),
                entity.getPrice(),
                entity.getSupplier()
        );
    }
}
