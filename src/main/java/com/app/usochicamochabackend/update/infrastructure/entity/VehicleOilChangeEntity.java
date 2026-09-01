package com.app.usochicamochabackend.update.infrastructure.entity;

import com.app.usochicamochabackend.vehicle.infrastructure.entity.VehicleEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "vehicle_oil_changes")
public class VehicleOilChangeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime dateStamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "oil_type")
    private OilType oilType;

    @ManyToOne
    @JoinColumn(name = "brand_id")
    private BrandEntity brand;

    private Double quantity;

    @Column(name = "km_at_change")
    private Integer kmAtChange;

    @Column(name = "interval_km")
    private Integer intervalKm;

    @ManyToOne
    @JoinColumn(name = "vehicle_id")
    private VehicleEntity vehicle;

    @Column(name = "air_filter_changed")
    private Boolean airFilterChanged;

    @Builder.Default
    private Boolean status = true;
}
