package com.app.usochicamochabackend.performance.application.dto;

public record ExecuteAnOrderRequest(
        Long orderId,
        Integer hoursSpent,
        Integer minutesSpent,
        String description,
        LaborRequest labor,
        SparePartRequest sparePart
) {}