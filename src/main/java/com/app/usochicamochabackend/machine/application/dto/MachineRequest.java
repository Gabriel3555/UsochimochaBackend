package com.app.usochicamochabackend.machine.application.dto;

import com.app.usochicamochabackend.common.text.InputTextNormalizer;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MachineRequest(String name, String belongsTo, String model, LocalDate soat, String brand, LocalDate runt, String numEngine, String numInterIdentification, BigDecimal fuelTankCapacityGallons, BigDecimal factoryEfficiencyGalPerHour, String factoryEfficiencyUnit) {
    public MachineRequest normalized() {
        return new MachineRequest(
                InputTextNormalizer.normalizeTitleWords(name),
                InputTextNormalizer.normalizeTitleWords(belongsTo),
                InputTextNormalizer.normalizeTitleWords(model),
                soat,
                InputTextNormalizer.normalizeTitleWords(brand),
                runt,
                InputTextNormalizer.normalizeIdCode(numEngine),
                InputTextNormalizer.normalizeIdCode(numInterIdentification),
                fuelTankCapacityGallons,
                factoryEfficiencyGalPerHour,
                factoryEfficiencyUnit);
    }
}
