package com.app.usochicamochabackend.order.application.dto;

import com.app.usochicamochabackend.review.application.dto.InspectionFormResponse;
import com.app.usochicamochabackend.user.application.dto.UserResponse;

import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        String status,
        LocalDateTime date,
        String description,
        InspectionFormResponse inspection,
        UserResponse assignerUser,
        String orderType,
        String maintenanceType,
        String consecutive
) {}