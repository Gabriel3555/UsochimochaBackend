package com.app.usochicamochabackend.performance.application.dto;

import java.math.BigDecimal;

public record SparePartResponse(Long id, String ref, String name, String quantity, BigDecimal price, String supplier) {}