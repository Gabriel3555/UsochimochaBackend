package com.app.usochicamochabackend.fuel.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "fuel_asset_config",
    uniqueConstraints = @UniqueConstraint(columnNames = {"asset_type", "asset_id"})
)
public class FuelAssetConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 10)
    private AssetType assetType;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "tank_capacity_liters", precision = 8, scale = 2)
    private BigDecimal tankCapacityLiters;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_unit", nullable = false, length = 10)
    @Builder.Default
    private QuantityUnit defaultUnit = QuantityUnit.LITERS;

    @Column(name = "monthly_budget_ref", precision = 14, scale = 2)
    private BigDecimal monthlyBudgetRef;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void onSave() {
        updatedAt = LocalDateTime.now();
    }
}
