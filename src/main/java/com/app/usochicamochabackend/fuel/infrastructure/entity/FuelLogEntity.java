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
@Table(name = "fuel_logs")
public class FuelLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sync_id", length = 36, unique = true)
    private String syncId;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 10)
    private AssetType assetType;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "asset_plate", length = 20)
    private String assetPlate;

    @Column(name = "fuel_date_time", nullable = false)
    private LocalDateTime fuelDateTime;

    @Column(name = "odometer_km", precision = 12, scale = 2)
    private BigDecimal odometerKm;

    @Column(name = "hour_meter", precision = 10, scale = 2)
    private BigDecimal hourMeter;

    @Column(name = "previous_odometer", precision = 12, scale = 2)
    private BigDecimal previousOdometer;

    @Column(name = "previous_hour_meter", precision = 10, scale = 2)
    private BigDecimal previousHourMeter;

    @Column(name = "delta_km", precision = 12, scale = 2)
    private BigDecimal deltaKm;

    @Column(name = "delta_hours", precision = 10, scale = 2)
    private BigDecimal deltaHours;

    @Column(name = "quantity", nullable = false, precision = 8, scale = 3)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "quantity_unit", nullable = false, length = 10)
    private QuantityUnit quantityUnit;

    @Column(name = "quantity_liters", precision = 8, scale = 3) // null para GAS_NATURAL (CUBIC_METERS)
    private BigDecimal quantityLiters;

    @Column(name = "price_per_unit", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerUnit;

    @Column(name = "total_cost_calculated", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCostCalculated;

    @Column(name = "total_cost_actual", precision = 12, scale = 2)
    private BigDecimal totalCostActual;

    @Column(name = "total_cost_mismatch", nullable = false)
    @Builder.Default
    private Boolean totalCostMismatch = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false, length = 25)
    private FuelType fuelType;

    @Column(name = "service_station", length = 150)
    private String serviceStation;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "invoice_photo_url", length = 500)
    private String invoicePhotoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_status", nullable = false, length = 20)
    @Builder.Default
    private InvoiceStatus invoiceStatus = InvoiceStatus.PENDING_REVIEW;

    @Column(name = "voucher_number", length = 50)
    private String voucherNumber;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "efficiency_value", precision = 10, scale = 4)
    private BigDecimal efficiencyValue;

    @Column(name = "efficiency_unit", length = 20)
    private String efficiencyUnit;

    @Column(name = "is_anomaly", nullable = false)
    @Builder.Default
    private Boolean isAnomaly = false;

    @Column(name = "registered_by", length = 100)
    private String registeredBy;

    @Column(name = "anomaly_dismissed_by", length = 100)
    private String anomalyDismissedBy;

    @Column(name = "anomaly_dismissed_at")
    private LocalDateTime anomalyDismissedAt;

    @Column(name = "anomaly_dismiss_reason", columnDefinition = "TEXT")
    private String anomalyDismissReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (fuelDateTime == null) {
            fuelDateTime = createdAt;
        }
    }
}
