package com.app.usochicamochabackend.auth.web;

import com.app.usochicamochabackend.auth.application.dto.*;
import com.app.usochicamochabackend.auth.application.port.LoginUseCase;
import com.app.usochicamochabackend.auth.application.port.RefreshTokenUseCase;
import com.app.usochicamochabackend.auth.utils.JwtUtils;
import com.app.usochicamochabackend.notifications.application.PreventiveAlertCalculationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LoginUseCase loginUseCase;

    @MockBean
    private RefreshTokenUseCase refreshTokenUseCase;

    @MockBean
    private PreventiveAlertCalculationService preventiveAlertCalculationService;

    @MockBean
    private JwtUtils jwtUtils;

    @Test
    @DisplayName("POST /api/v1/auth/login: credenciales válidas → 200 con JWT")
    void login_CredencialesValidas_Devuelve200() throws Exception {
        AuthResponse authResponse = new AuthResponse(
                1L, "admin", "Admin User", "ROLE_ADMIN", "OK",
                "mocked-jwt-token", "mocked-refresh-token", true);
        when(loginUseCase.login(any(AuthRequest.class))).thenReturn(authResponse);

        AuthRequest request = new AuthRequest("admin", "password");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.jwt").value("mocked-jwt-token"))
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login: credenciales inválidas → 401")
    void login_CredencialesInvalidas_Devuelve401() throws Exception {
        when(loginUseCase.login(any(AuthRequest.class)))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));

        AuthRequest request = new AuthRequest("admin", "wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/auth/token/refresh: token válido → 200 con nuevo access token")
    void refreshToken_TokenValido_Devuelve200() throws Exception {
        RefreshTokenResponse tokenResponse = new RefreshTokenResponse("new-access-token");
        when(refreshTokenUseCase.refreshToken(any(RefreshTokenRequest.class))).thenReturn(tokenResponse);

        RefreshTokenRequest request = new RefreshTokenRequest("mocked-refresh-token");

        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/token/refresh: token inválido → 401")
    void refreshToken_TokenInvalido_Devuelve401() throws Exception {
        when(refreshTokenUseCase.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "Token inválido"));

        RefreshTokenRequest request = new RefreshTokenRequest("invalid-token");

        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
