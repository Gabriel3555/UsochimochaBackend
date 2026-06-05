package com.app.usochicamochabackend.user.application.dto;

import java.time.LocalDate;

public record UpdateUserRequest(Long id, String fullName, Boolean status, String username, String email, String role,
                                String licenseCategory, LocalDate licenseExpiry) {}