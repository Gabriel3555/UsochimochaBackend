package com.app.usochicamochabackend.user.application.service;

import com.app.usochicamochabackend.actions.application.port.SaveActionUseCase;
import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import com.app.usochicamochabackend.auth.infrastructure.entity.UserEntity;
import com.app.usochicamochabackend.auth.infrastructure.repository.UserRepositoryJpa;
import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.exception.UserSoftDeletedConflictException;
import com.app.usochicamochabackend.mapper.UserMapper;
import com.app.usochicamochabackend.notifications.application.NotificationService;
import com.app.usochicamochabackend.notifications.application.PreventiveAlertCalculationService;
import com.app.usochicamochabackend.notifications.application.PreventiveAlertPersistenceService;
import com.app.usochicamochabackend.user.application.dto.*;
import com.app.usochicamochabackend.user.application.port.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService implements
        CreateUserUseCase,
        DeleteUserUseCase,
        FindAllUsersUseCase,
        FindUserByIdUseCase,
        UpdateUserUseCase,
        ChangePasswordUseCase {

    private static final long MAX_BYTES = 15 * 1024 * 1024L;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf");

    private final UserRepositoryJpa userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SaveActionUseCase saveActionUseCase;
    private final NotificationService notificationService;
    private final PreventiveAlertCalculationService preventiveAlertCalculationService;
    private final PreventiveAlertPersistenceService preventiveAlertPersistenceService;

    @Value("${app.storage.uploads-root:uploads}")
    private String uploadsRoot;

    @Override
    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {
        Optional<UserEntity> existingUser = userRepository.findByUsername(request.username());

        if (existingUser.isPresent()) {
            UserEntity existing = existingUser.get();
            if (existing.getStatus()) {
                throw new IllegalArgumentException("El usuario '" + request.username() + "' ya existe");
            } else {
                throw new UserSoftDeletedConflictException(
                    "El usuario '" + request.username() + "' fue eliminado. Opciones: restaurar con POST /{id}/restore o crear con otro nombre de usuario",
                    UserMapper.toResponse(existing)
                );
            }
        }

        Optional<UserEntity> existingEmail = userRepository.findActiveByEmail(request.email());
        if (existingEmail.isPresent()) {
            throw new IllegalArgumentException(
                "El email '" + request.email() + "' ya está registrado por otro usuario"
            );
        }

        UserEntity user = UserEntity.builder()
                .username(request.username())
                .fullName(request.fullName())
                .status(true)
                .email(request.email())
                .role(request.role())
                .password(passwordEncoder.encode(request.password()))
                .licenseCategory(request.licenseCategory())
                .licenseExpiry(request.licenseExpiry())
                .build();

        UserEntity userSaved = userRepository.save(user);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            saveActionUseCase.save("El usuario " + userSaved.getUsername() + " ha sido creado por " + userPrincipal.username());
        } else {
            saveActionUseCase.save("El usuario " + userSaved.getUsername() + " ha sido creado");
        }

        if (userSaved.getLicenseExpiry() != null) {
            userRepository.flush();
            preventiveAlertCalculationService.calculateAndEmitAlerts();
        }

        return new CreateUserResponse(user.getId(), userSaved.getFullName(), userSaved.getUsername(), userSaved.getEmail(), userSaved.getRole(), userSaved.getStatus(), "User created successfully");
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setStatus(false);
        userRepository.save(user);

        // El usuario deshabilitado ya no debe mostrar alertas de licencia activas.
        preventiveAlertPersistenceService.deleteActiveAlertForAssetAndSubtype(
                user.getUsername(), "DOCUMENTO", "LICENCIA");

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            saveActionUseCase.save("El usuario " + user.getUsername() + " ha sido eliminado por " + userPrincipal.username());
        } else {
            saveActionUseCase.save("El usuario " + user.getUsername() + " ha sido eliminado");
        }
    }

    @Transactional
    public UserResponse restoreUser(Long id) {
        UserEntity user = userRepository.findDeletedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deleted user not found with id: " + id));
        user.setStatus(true);
        UserEntity restored = userRepository.save(user);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            saveActionUseCase.save("El usuario " + user.getUsername() + " ha sido restaurado por " + userPrincipal.username());
        } else {
            saveActionUseCase.save("El usuario " + user.getUsername() + " ha sido restaurado");
        }

        return resolveUserResponse(UserMapper.toResponse(restored));
    }

    @Transactional
    public UserResponse restoreAndUpdateUser(Long id, RestoreUserRequest request) {
        UserEntity user = userRepository.findDeletedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deleted user not found with id: " + id));

        user.setStatus(true);

        if (request.restoreWithNewPassword() && request.newPassword() != null && !request.newPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.newPassword()));
        }

        UserEntity restored = userRepository.save(user);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String performer = "sistema";
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            performer = userPrincipal.username();
        }

        String mensaje = "El usuario " + user.getUsername() + " ha sido restaurado por " + performer;
        if (request.restoreWithNewPassword() && request.newPassword() != null && !request.newPassword().isBlank()) {
            mensaje += " con cambio de contraseña";
        }
        saveActionUseCase.save(mensaje);

        return resolveUserResponse(UserMapper.toResponse(restored));
    }

    @Override
    public UsersResponse findAllUsers() {
        List<UserEntity> userEntities = userRepository.findAllActive();
        List<UserResponse> resolved = UserMapper.toResponse(userEntities).users().stream()
                .map(this::resolveUserResponse)
                .toList();
        return new UsersResponse(resolved);
    }

    @Override
    public UserResponse findUserById(Long id) {
        UserEntity user = userRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return resolveUserResponse(UserMapper.toResponse(user));
    }

    @Override
    @Transactional
    public UserResponse updateUser(UpdateUserRequest request) throws ResourceNotFoundException, URISyntaxException {
        UserEntity currentUser = userRepository.findActiveById(request.id())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.id()));

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

        Optional.ofNullable(request.licenseCategory()).ifPresent(licenseCategory -> {
            currentUser.setLicenseCategory(licenseCategory);
            changes.add("licenseCategory");
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

        // Recalcular alertas de inmediato si cambió la licencia, para que la alerta previa
        // desaparezca/actualice sin esperar al cron diario ni a un refresh manual.
        if (changes.contains("licenseExpiry")) {
            userRepository.flush();
            preventiveAlertCalculationService.calculateAndEmitAlerts();
        }

        return resolveUserResponse(UserMapper.toResponse(userUpdated));
    }


    @Override
    @Transactional
    public ChangePasswordResponse changePassword(ChangePasswordRequest request) {
        UserEntity currentUser = userRepository.findActiveById(request.id())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.id()));

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

    public UserResponse uploadLicenseDocument(Long userId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Archivo vacío.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("El archivo supera el tamaño máximo permitido (15 MB).");
        }
        String mime = file.getContentType() != null ? file.getContentType().toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED_TYPES.contains(mime)) {
            throw new IllegalArgumentException("Tipo de archivo no permitido. Use JPEG, PNG, WebP o PDF.");
        }

        UserEntity user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        String ext = resolveExtension(file.getOriginalFilename(), mime);
        Path dir = Paths.get(uploadsRoot).toAbsolutePath().normalize()
                .resolve("users")
                .resolve(String.valueOf(userId))
                .resolve("licencia");
        Files.createDirectories(dir);
        Path target = dir.resolve("current" + ext);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        String relativeUrl = "/uploads/users/" + userId + "/licencia/current" + ext;
        user.setLicenseDocumentUrl(relativeUrl);
        UserEntity saved = userRepository.save(user);

        saveActionUseCase.save("Documento de licencia actualizado para el usuario " + saved.getUsername());
        return resolveUserResponse(UserMapper.toResponse(saved));
    }

    private UserResponse resolveUserResponse(UserResponse r) {
        if (r == null) return null;
        String resolved = resolveDocumentUrl(r.licenseDocumentUrl());
        if (resolved == null && r.licenseDocumentUrl() == null) return r;
        return new UserResponse(r.id(), r.username(), r.fullName(), r.email(), r.role(),
                r.licenseCategory(), r.licenseExpiry(), resolved);
    }

    private static String resolveDocumentUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) return null;
        if (rawUrl.toLowerCase().startsWith("http")) return rawUrl;
        try {
            String base = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
            String clean = rawUrl.replace("\\", "/");
            if (clean.startsWith("/")) clean = clean.substring(1);
            return base + "/" + clean;
        } catch (Exception e) {
            // Fuera de contexto HTTP (tests, background jobs): devolver tal cual
            return rawUrl;
        }
    }

    private static String resolveExtension(String originalFilename, String mime) {
        if (originalFilename != null && originalFilename.contains(".")) {
            String fromName = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
            if (fromName.length() <= 5 && fromName.matches("\\.(jpe?g|png|webp|pdf)")) {
                return fromName;
            }
        }
        return switch (mime) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "application/pdf" -> ".pdf";
            default -> ".bin";
        };
    }
}