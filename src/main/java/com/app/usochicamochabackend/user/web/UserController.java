package com.app.usochicamochabackend.user.web;

import com.app.usochicamochabackend.user.application.dto.*;
import com.app.usochicamochabackend.user.application.port.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.app.usochicamochabackend.user.application.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.app.usochicamochabackend.auth.application.dto.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Endpoints for managing users")
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final FindAllUsersUseCase findAllUsersUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final FindUserByIdUseCase findUserByIdUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user (own profile, license info)")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(findUserByIdUseCase.findUserById(principal.id()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(findUserByIdUseCase.findUserById(id));
    }

    @GetMapping
    @Operation(summary = "Get all users")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of users",
                    content = @Content(schema = @Schema(implementation = UsersResponse.class)))
    })
    public ResponseEntity<UsersResponse> getUsers() {
        return ResponseEntity.ok(findAllUsersUseCase.findAllUsers());
    }

    @PostMapping
    @Operation(summary = "Create user")
    @ApiResponse(responseCode = "201", description = "User created",
            content = @Content(schema = @Schema(implementation = CreateUserResponse.class)))
    public ResponseEntity<CreateUserResponse> createUser(
            @RequestBody(
                    description = "User data to create",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CreateUserRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "username": "jdoe",
                                      "fullName": "John Doe",
                                      "email": "john@example.com",
                                      "password": "secure123",
                                      "role": "USER"
                                    }
                                    """)
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody CreateUserRequest request
    ) throws URISyntaxException {
        CreateUserResponse saved = createUserUseCase.createUser(request);
        return ResponseEntity
                .created(new URI("/api/v1/user/" + saved.id()))
                .body(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update User")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestBody UpdateUserRequest user
    ) throws URISyntaxException {
        UserResponse updated = updateUserUseCase.updateUser(user);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete User")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        deleteUserUseCase.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/change-password")
    @Operation(
            summary = "Change a user's password",
            description = "Updates the password of a user identified by their ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password successfully changed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ChangePasswordResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<ChangePasswordResponse> changePassword(
            @PathVariable Long id,
            @RequestBody(
                    description = "Request containing new password",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ChangePasswordRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "newPassword": "newSecurePass123"
                                    }
                                    """)
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody ChangePasswordRequest request
    ) {
        ChangePasswordResponse response = changePasswordUseCase.changePassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{id}/license-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir documento de licencia de tránsito del usuario")
    public ResponseEntity<UserResponse> uploadLicenseDocument(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(userService.uploadLicenseDocument(id, file));
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore a deleted (soft-deleted) user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User restored",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> restoreUser(@PathVariable Long id) {
        UserResponse restored = userService.restoreUser(id);
        return ResponseEntity.ok(restored);
    }

    @PostMapping("/{id}/restore-with-password")
    @Operation(summary = "Restore a deleted user and optionally update password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User restored",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> restoreAndUpdateUser(
            @PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestBody RestoreUserRequest request
    ) {
        UserResponse restored = userService.restoreAndUpdateUser(id, request);
        return ResponseEntity.ok(restored);
    }
}
