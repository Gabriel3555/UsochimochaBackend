package com.app.usochicamochabackend.order.infrastructure.repository;

import com.app.usochicamochabackend.order.infrastructure.entity.OrderCounterEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderCounterRepository extends JpaRepository<OrderCounterEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM OrderCounterEntity c WHERE c.moduleCode = :code")
    Optional<OrderCounterEntity> findByModuleCodeForUpdate(@Param("code") String code);
}
