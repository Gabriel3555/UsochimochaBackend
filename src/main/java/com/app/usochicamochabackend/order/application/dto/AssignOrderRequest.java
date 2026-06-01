package com.app.usochicamochabackend.order.application.dto;

public record AssignOrderRequest(Long inspectionId /* Long assignedUserId */, String description, String orderType, String maintenanceType) {}