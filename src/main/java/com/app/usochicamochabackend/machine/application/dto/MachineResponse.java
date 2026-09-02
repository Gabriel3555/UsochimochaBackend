package com.app.usochicamochabackend.machine.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MachineResponse(Long id, String name, String belongsTo, String model, LocalDate soat, String brand, LocalDate runt, String numEngine, String numInterIdentification, BigDecimal fuelTankCapacityGallons, BigDecimal factoryEfficiencyGalPerHour, String factoryEfficiencyUnit, Integer horometroActual) {}
