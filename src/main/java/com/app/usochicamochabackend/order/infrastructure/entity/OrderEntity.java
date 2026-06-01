package com.app.usochicamochabackend.order.infrastructure.entity;

import com.app.usochicamochabackend.auth.infrastructure.entity.UserEntity;
import com.app.usochicamochabackend.performance.infrastructure.entity.ResultEntity;
import com.app.usochicamochabackend.review.infrastructure.entity.InspectionEntity;
import com.app.usochicamochabackend.vehicleinspection.infrastructure.entity.InspPreOperativaEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String status;

    private LocalDateTime date;

    private String description;

    @ManyToOne
    @JoinColumn(name = "inspection_id")
    private InspectionEntity inspection;

    @ManyToOne
    @JoinColumn(name = "vehicle_inspection_id", nullable = true)
    private InspPreOperativaEntity vehicleInspection;

    @OneToOne
    @JoinColumn(name = "result_id", nullable = true)
    private ResultEntity result;

    @ManyToOne
    @JoinColumn(name = "assigner_user_id")
    private UserEntity assignerUser;

    @Column(name = "order_type", length = 50)
    private String orderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_type", length = 30)
    private MaintenanceType maintenanceType;

    @Column(unique = true, length = 20)
    private String consecutive;

    @PrePersist
    protected void onCreate() {
        this.date = LocalDateTime.now();
    }
}
