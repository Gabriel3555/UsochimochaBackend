package com.app.usochicamochabackend.user.application.dto;

import java.time.LocalDate;

public record UserResponse(Long id, String username, String fullName, String email, String role,
                           String licenseNumber, LocalDate licenseExpiry) {
}
