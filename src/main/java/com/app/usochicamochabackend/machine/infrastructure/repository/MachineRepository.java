package com.app.usochicamochabackend.machine.infrastructure.repository;

import com.app.usochicamochabackend.machine.infrastructure.entity.MachineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MachineRepository extends JpaRepository<MachineEntity, Long> {
    Optional<MachineEntity> findByName(String name);
}
