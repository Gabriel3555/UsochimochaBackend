package com.app.usochicamochabackend.fuel.infrastructure.repository;

import com.app.usochicamochabackend.fuel.infrastructure.entity.RefuelingRecordsEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefuelingRecordsRepository extends JpaRepository<RefuelingRecordsEntity, Long> {
    Page<RefuelingRecordsEntity> findByStatus(Boolean status, Pageable pageable);
}
