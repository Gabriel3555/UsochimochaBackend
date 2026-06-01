package com.app.usochicamochabackend.order.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "order_counters")
public class OrderCounterEntity {

    @Id
    @Column(name = "module_code", length = 5)
    private String moduleCode;  // 'VH', 'MQ', 'MT'

    @Column(name = "last_value", nullable = false)
    private Integer lastValue = 0;
}
