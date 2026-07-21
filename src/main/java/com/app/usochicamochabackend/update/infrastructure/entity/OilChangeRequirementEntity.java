package com.app.usochicamochabackend.update.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad que define los rangos de cambio de aceite según:
 * - Tipo de aceite (Mineral, Semicintético, 100% Sintético)
 * - Tipo de activo (Vehículo, Motocicleta, Maquinaria)
 *
 * Basado en estándares reales de fabricantes (Caterpillar, John Deere, Komatsu, etc.)
 */
@Entity
@Table(name = "oil_change_requirements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OilChangeRequirementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OilType oilType;              // MINERAL, SEMI_SYNTHETIC, SYNTHETIC

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetType assetType;          // VEHICLE, MOTORCYCLE, MACHINERY

    @Column(name = "km_range")
    private Integer kmRange;              // Para vehículos/motos (en km)

    @Column(name = "hour_range")
    private Integer hourRange;            // Para maquinaria (en horas)

    @Column(nullable = false)
    private String description;           // "100% Sintético - Motocicleta - 10000km"

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Obtiene el valor del rango (km o horas según corresponda)
     */
    public Integer getRangeValue() {
        return this.kmRange != null ? this.kmRange : this.hourRange;
    }

    /**
     * Obtiene la unidad del rango (km o horas)
     */
    public String getRangeUnit() {
        return this.kmRange != null ? "km" : "hrs";
    }

    /**
     * Valida que solo uno de kmRange o hourRange esté configurado
     */
    @PostLoad
    private void validate() {
        if ((this.kmRange == null && this.hourRange == null) ||
            (this.kmRange != null && this.hourRange != null)) {
            throw new IllegalArgumentException(
                "OilChangeRequirement debe tener SOLO kmRange (para vehículos/motos) O hourRange (para maquinaria)"
            );
        }
    }
}
