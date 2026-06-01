package com.app.usochicamochabackend.performance.infrastructure.entity;

import com.app.usochicamochabackend.order.infrastructure.entity.OrderEntity;
import com.app.usochicamochabackend.review.infrastructure.entity.InspectionEntity;
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
@Table(name = "results")
public class ResultEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime date;

    private String description;

    private String timeSpent;

    @Column(name = "hours_spent")
    private Integer hoursSpent;

    @Column(name = "minutes_spent")
    private Integer minutesSpent;

    @OneToOne(mappedBy = "result")
    private OrderEntity order;

    @OneToOne(cascade = CascadeType.ALL)
    private LaborEntity laborForce;

    @OneToOne(cascade = CascadeType.ALL)
    private SparePartEntity sparePart;
}