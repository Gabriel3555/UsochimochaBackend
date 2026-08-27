package com.app.usochicamochabackend.update.infrastructure.entity;

/**
 * Tipos de aceite para maquinaria y vehículos
 * - MOTOR: Aceite de motor (para motores de combustión interna)
 * - HYDRAULIC: Aceite hidráulico (para sistemas hidráulicos)
 */
public enum OilType {
    MOTOR("Aceite Motor"),
    HYDRAULIC("Aceite Hidráulico"),
    MINERAL("Aceite Mineral"),
    SEMI_SYNTHETIC("Aceite Semicintético"),
    SYNTHETIC("Aceite Sintético");

    private final String displayName;

    OilType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Convierte valores variados (de mobile, web, etc.) a OilType enum
     * Acepta: "motor", "MOTOR", "aceite motor", "hydraulic", "HYDRAULIC", "aceite hidraulico", etc.
     */
    public static OilType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El tipo de aceite no puede estar vacío");
        }

        String normalized = value.trim().toUpperCase()
                .replace("ACEITE", "")
                .replace("-", "")
                .replace(" ", "")
                .replace("_", "")
                .trim();

        // Mapear valores simplificados
        if (normalized.isEmpty() || normalized.equals("MOTOR")) {
            return MOTOR;
        }
        if (normalized.equals("HYDRAULIC") || normalized.equals("HIDRAULICO") || normalized.equals("HIDRAULICA")) {
            return HYDRAULIC;
        }

        if (normalized.equals("MINERAL")) return MINERAL;
        if (normalized.equals("SEMISYNTHETIC") || normalized.contains("SEMI")) return SEMI_SYNTHETIC;
        if (normalized.equals("SYNTHETIC") || normalized.equals("SINTETICO") || normalized.contains("SINT")) return SYNTHETIC;

        // Si no coincide, lanzar error descriptivo
        throw new IllegalArgumentException(
                "Tipo de aceite no válido: '" + value + "'. Debe ser uno de: MOTOR, HYDRAULIC, MINERAL, SEMI_SYNTHETIC, SYNTHETIC");
    }
}
