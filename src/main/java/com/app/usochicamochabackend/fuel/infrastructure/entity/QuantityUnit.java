package com.app.usochicamochabackend.fuel.infrastructure.entity;

public enum QuantityUnit {
    LITERS,
    GALLONS,
    CUBIC_METERS; // Gas natural — no se normaliza a litros

    private static final double LITERS_PER_GALLON = 3.785411784;

    /** Retorna null para CUBIC_METERS (gas no es comparable con líquidos). */
    public Double toLiters(double quantity) {
        return switch (this) {
            case GALLONS -> quantity * LITERS_PER_GALLON;
            case LITERS  -> quantity;
            case CUBIC_METERS -> null;
        };
    }

    public double toGallons(double quantity) {
        return this == LITERS ? quantity / LITERS_PER_GALLON : quantity;
    }
}
