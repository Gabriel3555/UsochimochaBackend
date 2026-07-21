package com.app.usochicamochabackend.order.application.dto;

import com.app.usochicamochabackend.user.application.dto.UserResponse;

import java.time.LocalDateTime;

public record OrderWithoutInspectionResponse(
        Long id,
        String status,
        LocalDateTime date,
        String description,
        UserResponse assignerUser,
        String orderType,
        String maintenanceType,
        String maintenanceCategory,
        String consecutive,
        Integer hoursSpent,
        Integer minutesSpent,
        String suppliers,
        String timeSpent
) {}