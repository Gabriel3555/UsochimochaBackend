package com.app.usochicamochabackend.auth.web;

import com.app.usochicamochabackend.auth.application.dto.AuthRequest;
import com.app.usochicamochabackend.auth.application.dto.AuthResponse;
import com.app.usochicamochabackend.auth.application.dto.RefreshTokenRequest;
import com.app.usochicamochabackend.auth.application.dto.RefreshTokenResponse;
import com.app.usochicamochabackend.auth.application.port.LoginUseCase;
import com.app.usochicamochabackend.auth.application.port.RefreshTokenUseCase;
import com.app.usochicamochabackend.notifications.application.PreventiveAlertCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Auth", description = "Authentication endpoints")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final PreventiveAlertCalculationService preventiveAlertCalculationService;

    @Operation(
            summary = "Sign in",
            description = "Validates credentials and returns an access token and a refresh token"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Successful login",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
    )
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @PostMapping("/login")
    AuthResponse authenticate(@RequestBody @Valid AuthRequest authRequest) {
        AuthResponse response = loginUseCase.login(authRequest);

        // Dispara el cálculo de alertas preventivas al login exitoso
        try {
            log.info("🔔 [LOGIN] Calculando alertas preventivas para usuario: {}", authRequest.username());
            preventiveAlertCalculationService.calculateAndEmitAlerts();
            log.info("✅ [LOGIN] Alertas calculadas y emitidas por WebSocket");
        } catch (Exception e) {
            log.warn("⚠️ [LOGIN] Error al calcular alertas: {}", e.getMessage());
            // No fallar el login si hay error en alertas
        }

        return response;
    }

    @Operation(
            summary = "Refresh access token",
            description = "Generates a new access token using a valid refresh token"
    )
    @ApiResponse(
            responseCode = "200",
            description = "New access token issued",
            content = @Content(schema = @Schema(implementation = RefreshTokenResponse.class))
    )
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    @PostMapping("/token/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        return ResponseEntity.ok(refreshTokenUseCase.refreshToken(request));
    }
}