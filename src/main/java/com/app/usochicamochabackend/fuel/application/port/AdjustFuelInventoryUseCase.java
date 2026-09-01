package com.app.usochicamochabackend.fuel.application.port;

import java.math.BigDecimal;

public interface AdjustFuelInventoryUseCase {
    void increment(String areaCosto, Long fuelTypeId, BigDecimal cantidad);
    void decrement(String areaCosto, Long fuelTypeId, BigDecimal cantidad);
}
