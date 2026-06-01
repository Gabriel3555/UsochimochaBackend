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
    name = "fuel_stats_monthly",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_fuel_stats_month_asset_fuel",
        columnNames = {"year_month", "asset_type", "fuel_type"}
    ),
    indexes = @Index(name = "idx_fuel_stats_month", columnList = "year_month, asset_type")
)
public class FuelStatsMonthlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth; // formato "YYYY-MM"

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 10)
    private AssetType assetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false, length = 25)
    private FuelType fuelType;

    @Column(name = "total_gallons", precision = 12, scale = 4)
    private BigDecimal totalGallons;

    @Column(name = "total_cost", precision = 15, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "total_loads")
    private Integer totalLoads;

    @Column(name = "anomaly_count")
    private Integer anomalyCount;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
