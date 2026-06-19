package com.app.usochicamochabackend.auth.infrastructure.repository;

import com.app.usochicamochabackend.auth.infrastructure.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepositoryJpa extends JpaRepository<UserEntity, Long> {

    // ✅ NUEVA: Solo usuarios activos por username
    @Query("SELECT u FROM UserEntity u WHERE u.status = true AND u.username = :username")
    Optional<UserEntity> findActiveByUsername(@Param("username") String username);

    // ⚠️ DEPRECATED: Usar findActiveByUsername() en lugar de este método
    // Mantener solo para casos especiales (admin, auditoría)
    Optional<UserEntity> findByUsername(String username);

    // ✅ NUEVA: Solo usuarios activos por ID
    @Query("SELECT u FROM UserEntity u WHERE u.status = true AND u.id = :id")
    Optional<UserEntity> findActiveById(@Param("id") Long id);

    // ✅ NUEVA: Lista eficiente de usuarios activos (filtro a nivel DB)
    @Query("SELECT u FROM UserEntity u WHERE u.status = true ORDER BY u.username ASC")
    List<UserEntity> findAllActive();

    // ✅ NUEVA: Buscar usuarios eliminados para restauración (admin only)
    @Query("SELECT u FROM UserEntity u WHERE u.status = false AND u.id = :id")
    Optional<UserEntity> findDeletedById(@Param("id") Long id);

    // ✅ NUEVA: Buscar usuario activo por email (prevenir duplicados)
    @Query("SELECT u FROM UserEntity u WHERE u.status = true AND u.email = :email")
    Optional<UserEntity> findActiveByEmail(@Param("email") String email);

    // ⚠️ DEPRECATED: Usar findActiveById() en lugar de este método
    @Deprecated(forRemoval = true, since = "1.0")
    UserEntity getUserEntityById(Long id);
}
