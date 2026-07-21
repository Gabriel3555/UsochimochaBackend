package com.app.usochicamochabackend.mapper;

import com.app.usochicamochabackend.auth.infrastructure.entity.UserEntity;
import com.app.usochicamochabackend.user.application.dto.UserResponse;
import com.app.usochicamochabackend.user.application.dto.UsersResponse;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UserMapper {

    private UserMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static UserResponse toResponse(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        return new UserResponse(
                entity.getId(),
                entity.getUsername(),
                entity.getFullName(),
                entity.getEmail(),
                entity.getRole(),
                entity.getLicenseCategory(),
                entity.getLicenseExpiry(),
                entity.getLicenseDocumentUrl()
        );
    }

    public static UsersResponse toResponse(List<UserEntity> entities) {
        if (entities == null) {
            return new UsersResponse(List.of());
        }

        List<UserResponse> userResponses = entities.stream()
                .filter(Objects::nonNull)
                .map(UserMapper::toResponse)
                .collect(Collectors.toList());

        return new UsersResponse(userResponses);
    }

}
