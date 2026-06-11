package com.app.usochicamochabackend.update.infrastructure.entity;

/**
 * Tipos de aceite basados en estándares reales
 * - MINERAL: Aceites convencionales (15W-40)
 * - SEMI_SYNTHETIC: Mezcla mineral-sintética con aditivos mejorados
 * - SYNTHETIC: Aceites 100% sintéticos para máxima protección
 */
public enum OilType {
    MINERAL("Mineral", 1),
    SEMI_SYNTHETIC("Semicintético", 2),
    SYNTHETIC("100% Sintético", 3);

    private final String displayName;
    private final Integer level;  // Para ordenar por calidad/durabilidad

    OilType(String displayName, Integer level) {
        this.displayName = displayName;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Integer getLevel() {
        return level;
    }

    /**
     * Retorna el nivel de durabilidad (a mayor número, mayor duración)
     */
    public String getDurabilityLevel() {
        return switch (this) {
            case MINERAL -> "Baja";
            case SEMI_SYNTHETIC -> "Media-Alta";
            case SYNTHETIC -> "Muy Alta";
        };
    }
}
