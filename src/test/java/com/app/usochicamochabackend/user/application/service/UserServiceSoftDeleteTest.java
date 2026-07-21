package com.app.usochicamochabackend.user.application.service;

import com.app.usochicamochabackend.auth.infrastructure.entity.UserEntity;
import com.app.usochicamochabackend.auth.infrastructure.repository.UserRepositoryJpa;
import com.app.usochicamochabackend.exception.ResourceNotFoundException;
import com.app.usochicamochabackend.exception.UserSoftDeletedConflictException;
import com.app.usochicamochabackend.user.application.dto.CreateUserRequest;
import com.app.usochicamochabackend.user.application.dto.RestoreUserRequest;
import com.app.usochicamochabackend.user.application.dto.UpdateUserRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceSoftDeleteTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepositoryJpa userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void testSoftDeletedUserNotFoundByActiveQuery() {
        UserEntity user = userRepository.save(UserEntity.builder()
            .username("active_user")
            .fullName("Active User")
            .status(true)
            .password(passwordEncoder.encode("pass123"))
            .email("active@test.com")
            .role("USER")
            .build());

        userService.deleteUser(user.getId());

        Optional<UserEntity> found = userRepository.findActiveById(user.getId());
        assertThat(found).isEmpty();
    }

    @Test
    void testFindAllUsersExcludesSoftDeleted() {
        UserEntity active = userRepository.save(UserEntity.builder()
            .username("jane")
            .fullName("Jane Doe")
            .status(true)
            .password(passwordEncoder.encode("pass"))
            .email("jane@test.com")
            .role("USER")
            .build());

        UserEntity deleted = userRepository.save(UserEntity.builder()
            .username("john")
            .fullName("John Doe")
            .status(false)
            .password(passwordEncoder.encode("pass"))
            .email("john@test.com")
            .role("USER")
            .build());

        var response = userService.findAllUsers();

        assertThat(response.users())
            .extracting(r -> r.username())
            .contains("jane")
            .doesNotContain("john");
    }

    @Test
    void testCannotFindSoftDeletedUserById() {
        UserEntity user = userRepository.save(UserEntity.builder()
            .username("deleted_user")
            .fullName("Deleted User")
            .status(false)
            .password(passwordEncoder.encode("pass"))
            .email("deleted@test.com")
            .role("USER")
            .build());

        assertThatThrownBy(() -> userService.findUserById(user.getId()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    void testCanRestoreSoftDeletedUser() {
        UserEntity user = userRepository.save(UserEntity.builder()
            .username("to_restore")
            .fullName("To Restore")
            .status(false)
            .password(passwordEncoder.encode("pass"))
            .email("restore@test.com")
            .role("USER")
            .build());

        var restored = userService.restoreUser(user.getId());

        assertThat(restored.username()).isEqualTo("to_restore");
        Optional<UserEntity> found = userRepository.findActiveById(user.getId());
        assertThat(found).isPresent();
    }

    @Test
    void testCreateUserWithSoftDeletedUsernameThrowsConflict() {
        UserEntity deleted = userRepository.save(UserEntity.builder()
            .username("conflict_user")
            .fullName("Conflict User")
            .status(false)
            .password(passwordEncoder.encode("pass"))
            .email("conflict@test.com")
            .role("USER")
            .build());

        CreateUserRequest request = new CreateUserRequest(
            "conflict_user",
            "New Person",
            "new@test.com",
            "newpass123",
            "USER",
            null,
            null
        );

        assertThatThrownBy(() -> userService.createUser(request))
            .isInstanceOf(UserSoftDeletedConflictException.class)
            .hasMessageContaining("fue eliminado");
    }

    @Test
    void testCreateUserWithActiveUsernameThrowsIllegalArgument() {
        userRepository.save(UserEntity.builder()
            .username("active_user")
            .fullName("Active User")
            .status(true)
            .password(passwordEncoder.encode("pass"))
            .email("active@test.com")
            .role("USER")
            .build());

        CreateUserRequest request = new CreateUserRequest(
            "active_user",
            "Another Person",
            "another@test.com",
            "pass123",
            "USER",
            null,
            null
        );

        assertThatThrownBy(() -> userService.createUser(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ya existe");
    }

    @Test
    void testUpdateUserFailsIfSoftDeleted() {
        UserEntity deleted = userRepository.save(UserEntity.builder()
            .username("deleted_user")
            .fullName("Deleted User")
            .status(false)
            .password(passwordEncoder.encode("pass"))
            .email("deleted@test.com")
            .role("USER")
            .build());

        UpdateUserRequest request = new UpdateUserRequest(
            deleted.getId(),
            "New Name",
            null,
            "new_username",
            null,
            null,
            null,
            null
        );

        assertThatThrownBy(() -> userService.updateUser(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    void testCanRestoreAndUpdateUserWithPassword() {
        UserEntity deleted = userRepository.save(UserEntity.builder()
            .username("restore_password_user")
            .fullName("Restore Password User")
            .status(false)
            .password(passwordEncoder.encode("oldpass"))
            .email("restore@test.com")
            .role("USER")
            .build());

        RestoreUserRequest request = new RestoreUserRequest(
            deleted.getId(),
            true,
            "newpass123"
        );

        var restored = userService.restoreAndUpdateUser(deleted.getId(), request);

        assertThat(restored.username()).isEqualTo("restore_password_user");
        Optional<UserEntity> found = userRepository.findActiveById(deleted.getId());
        assertThat(found).isPresent();

        UserEntity foundUser = found.get();
        assertThat(passwordEncoder.matches("newpass123", foundUser.getPassword())).isTrue();
    }

    @Test
    void testLoginFailsWithSoftDeletedUser() {
        UserEntity active = userRepository.save(UserEntity.builder()
            .username("will_delete")
            .fullName("Will Delete")
            .status(true)
            .password(passwordEncoder.encode("pass123"))
            .email("willdelete@test.com")
            .role("USER")
            .build());

        userService.deleteUser(active.getId());

        Optional<UserEntity> found = userRepository.findActiveByUsername("will_delete");
        assertThat(found).isEmpty();
    }

    @Test
    void testFindByUsernameStillFindsDeletedForAdmin() {
        UserEntity user = userRepository.save(UserEntity.builder()
            .username("admin_only_user")
            .fullName("Admin Only")
            .status(false)
            .password(passwordEncoder.encode("pass"))
            .email("admin@test.com")
            .role("ADMIN")
            .build());

        Optional<UserEntity> found = userRepository.findByUsername("admin_only_user");
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isFalse();
    }

    @Test
    void testFindDeletedByIdReturnsOnlyDeleted() {
        UserEntity active = userRepository.save(UserEntity.builder()
            .username("active_one")
            .fullName("Active One")
            .status(true)
            .password(passwordEncoder.encode("pass"))
            .email("active1@test.com")
            .role("USER")
            .build());

        UserEntity deleted = userRepository.save(UserEntity.builder()
            .username("deleted_one")
            .fullName("Deleted One")
            .status(false)
            .password(passwordEncoder.encode("pass"))
            .email("deleted1@test.com")
            .role("USER")
            .build());

        Optional<UserEntity> foundActive = userRepository.findDeletedById(active.getId());
        Optional<UserEntity> foundDeleted = userRepository.findDeletedById(deleted.getId());

        assertThat(foundActive).isEmpty();
        assertThat(foundDeleted).isPresent();
    }

    @Test
    void testCreateUserWithDuplicateEmailThrowsIllegalArgument() {
        userRepository.save(UserEntity.builder()
            .username("user_one")
            .fullName("User One")
            .status(true)
            .password(passwordEncoder.encode("pass"))
            .email("shared@test.com")
            .role("USER")
            .build());

        CreateUserRequest request = new CreateUserRequest(
            "user_two",
            "User Two",
            "USER",
            "shared@test.com",
            "pass123",
            null,
            null
        );

        assertThatThrownBy(() -> userService.createUser(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("email")
            .hasMessageContaining("registrado");
    }

    @Test
    void testCreateUserWithEmailOfSoftDeletedUserAllowed() {
        userRepository.save(UserEntity.builder()
            .username("deleted_user")
            .fullName("Deleted User")
            .status(false)
            .password(passwordEncoder.encode("pass"))
            .email("old@test.com")
            .role("USER")
            .build());

        CreateUserRequest request = new CreateUserRequest(
            "new_user",
            "New User",
            "USER",
            "old@test.com",
            "pass123",
            null,
            null
        );

        var response = userService.createUser(request);

        assertThat(response.username()).isEqualTo("new_user");
        assertThat(response.email()).isEqualTo("old@test.com");

        Optional<UserEntity> created = userRepository.findActiveByEmail("old@test.com");
        assertThat(created).isPresent();
        assertThat(created.get().getUsername()).isEqualTo("new_user");
    }
}
