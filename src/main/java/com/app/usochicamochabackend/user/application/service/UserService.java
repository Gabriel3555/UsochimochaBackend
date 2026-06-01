package com.app.usochicamochabackend.user.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.auth.infrastructure.entity.UserEntity;
import com.app.usochicamochabackend.auth.infrastructure.repository.UserRepositoryJpa;
import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.mapper.UserMapper;
import com.app.usochicamochabackend.notifications.application.NotificationService;
import com.app.usochicamochabackend.user.application.dto.*;
import com.app.usochicamochabackend.user.application.port.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements
        CreateUserUseCase,
        DeleteUserUseCase,
        FindAllUsersUseCase,
        FindUserByIdUseCase,
        UpdateUserUseCase,
        ChangePasswordUseCase {

    private final UserRepositoryJpa userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SaveActionUseCase saveActionUseCase;
    private final NotificationService notificationService;

    @Override
    public CreateUserResponse createUser(CreateUserRequest request) {
        UserEntity user = UserEntity.builder()
                .username(request.username())
                .fullName(request.fullName())
                .status(true)
                .email(request.email())
                .role(request.role())
                .password(passwordEncoder.encode(request.password()))
                .licenseNumber(request.licenseNumber())
                .licenseExpiry(request.licenseExpiry())
                .build();

        UserEntity userSaved = userRepository.save(user);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            saveActionUseCase.save("El usuario " + userSaved.getUsername() + " ha sido creado por " + userPrincipal.username());
        } else {
            saveActionUseCase.save("El usuario " + userSaved.getUsername() + " ha sido creado");
        }

        return new CreateUserResponse(user.getId(), userSaved.getFullName(), userSaved.getUsername(), userSaved.getEmail(), userSaved.getRole(), userSaved.getStatus(), "User created successfully");
    }

    @Override
    public void deleteUser(Long id) {
        UserEntity user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setStatus(false);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            saveActionUseCase.save("El usuario " + user.getUsername() + " ha sido eliminado por " + userPrincipal.username());
        } else {
            saveActionUseCase.save("El usuario " + user.getUsername() + " ha sido eliminado");
        }

        userRepository.save(user);
    }

    @Override
    public UsersResponse findAllUsers() {
        List<UserEntity> userEntities = userRepository.findAll()
                .stream()
                .filter(UserEntity::getStatus)
                .toList();


        return UserMapper.toResponse(userEntities);
    }

    @Override
    public UserResponse findUserById(Long id) {
         UserEntity user = userRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("User not found with id: " + id));


         return UserMapper.toResponse(user);
    }

    @Override
    public UserResponse updateUser(UpdateUserRequest request) throws ResourceNotFoundException, URISyntaxException {
        UserEntity currentUser = userRepository.getUserEntityById(request.id());

        List<String> changes = new ArrayList<>();

        Optional.ofNullable(request.username()).ifPresent(username -> {
            currentUser.setUsername(username);
            changes.add("username");
        });

        Optional.ofNullable(request.fullName()).ifPresent(fullName -> {
            currentUser.setFullName(fullName);
            changes.add("fullName");
        });

        Optional.ofNullable(request.email()).ifPresent(email -> {
            currentUser.setEmail(email);
            changes.add("email");
        });

        Optional.ofNullable(request.role()).ifPresent(role -> {
            currentUser.setRole(role);
            changes.add("role");
        });

        Optional.ofNullable(request.licenseNumber()).ifPresent(licenseNumber -> {
            currentUser.setLicenseNumber(licenseNumber);
            changes.add("licenseNumber");
        });

        Optional.ofNullable(request.licenseExpiry()).ifPresent(licenseExpiry -> {
            currentUser.setLicenseExpiry(licenseExpiry);
            changes.add("licenseExpiry");
        });

        UserEntity userUpdated = userRepository.save(currentUser);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String performer = "sistema";
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            performer = userPrincipal.username();
        }

        String mensaje = "El usuario " + userUpdated.getUsername() +
                " ha sido actualizado por " + performer;

        if (!changes.isEmpty()) {
            mensaje += ". Campos modificados: " + String.join(", ", changes);
        }

        saveActionUseCase.save(mensaje);


        return UserMapper.toResponse(userUpdated);
    }


    @Override
    public ChangePasswordResponse changePassword(ChangePasswordRequest request) {
        UserEntity currentUser = userRepository.findById(request.id()).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.id()));

        currentUser.setPassword(passwordEncoder.encode(request.newPassword()));
        UserResponse userUpdated = UserMapper.toResponse(userRepository.save(currentUser));

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String performer = "sistema";
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            performer = userPrincipal.username();
        }

        saveActionUseCase.save("La contraseña del " + currentUser.getUsername() + " ha sido actualizada por " + performer);


        return new ChangePasswordResponse(userUpdated, "Password was change successfully", true);
    }
}