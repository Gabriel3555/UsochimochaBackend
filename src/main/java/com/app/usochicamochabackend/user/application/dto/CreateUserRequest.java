package com.app.usochicamochabackend.user.application.dto;

import java.time.LocalDate;

public record CreateUserRequest(String username, String fullName, String role, String email, String password,
                                String licenseCategory, LocalDate licenseExpiry) {
}
