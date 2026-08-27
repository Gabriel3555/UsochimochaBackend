package com.app.usochicamochabackend.fuel.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record FuelWarehouseBalanceResponse(List<Saldo> saldos) {
    public record Saldo(String areaCosto, Long fuelTypeId, BigDecimal cantidadDisponible) {}
}
