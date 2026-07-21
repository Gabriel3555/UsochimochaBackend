package com.app.usochicamochabackend;

import com.app.usochicamochabackend.auth.application.dto.AuthRequest;
import com.app.usochicamochabackend.auth.application.dto.AuthResponse;
import com.app.usochicamochabackend.auth.application.port.LoginUseCase;
import com.app.usochicamochabackend.user.application.dto.CreateUserRequest;
import com.app.usochicamochabackend.user.application.dto.CreateUserResponse;
import com.app.usochicamochabackend.user.application.port.CreateUserUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class UsochicamochaBackendApplicationTests {

    @Autowired
    private CreateUserUseCase createUserUseCase;

    @Autowired
    private LoginUseCase loginUseCase;

    @Test
    void contextLoads() {
        // Test that the Spring context loads successfully
    }

    @Test
    @DirtiesContext
    void applicationStartsSuccessfully() {
        // Test that the application starts without errors
        // This is an integration test that verifies all beans are properly configured
    }

    @Test
    @DirtiesContext
    void createUserAndLogin_ShouldWorkEndToEnd() {
        // Given - Create a new user
        CreateUserRequest createRequest = new CreateUserRequest(
                "integrationuser",
                "Integration Test User",
                "MECHANIC",
                "integration@example.com",
                "password123",
                "A",
                java.time.LocalDate.of(2027, 12, 31)
        );

        // When - Create user
        CreateUserResponse createResponse = createUserUseCase.createUser(createRequest);

        // Then - Verify user creation
        assertNotNull(createResponse);
        assertEquals("Integration Test User", createResponse.fullName());
        assertEquals("integrationuser", createResponse.username());
        assertTrue(createResponse.status());

        // When - Login with the created user
        AuthRequest loginRequest = new AuthRequest("integrationuser", "password123");
        AuthResponse loginResponse = loginUseCase.login(loginRequest);

        // Then - Verify login
        assertNotNull(loginResponse);
        assertEquals("integrationuser", loginResponse.username());
        assertNotNull(loginResponse.jwt());
        assertNotNull(loginResponse.refreshToken());
    }
}
