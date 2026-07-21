package com.app.usochicamochabackend.update.infrastructure.entity;

/**
 * Tipos de activos que requieren cambio de aceite
 * - VEHICLE: Automóviles/carros de pasajeros o carga liviana
 * - MOTORCYCLE: Motocicletas y vehículos de dos ruedas
 * - MACHINERY: Maquinaria pesada, agrícola, o industrial
 */
public enum AssetType {
    VEHICLE("Vehículo", "km", "Automóvil, camioneta, camión"),
    MOTORCYCLE("Motocicleta", "km", "Motocicleta, moto, vehículo de 2 ruedas"),
    MACHINERY("Maquinaria", "hrs", "Maquinaria pesada, tractor, excavadora, equipo industrial");

    private final String displayName;
    private final String unit;            // "km" o "hrs"
    private final String description;

    AssetType(String displayName, String unit, String description) {
        this.displayName = displayName;
        this.unit = unit;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUnit() {
        return unit;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Determina si usa km o horas para medir cambios de aceite
     */
    public boolean usesHourimetry() {
        return this == MACHINERY;
    }

    public boolean usesKilometry() {
        return this == VEHICLE || this == MOTORCYCLE;
    }
}
