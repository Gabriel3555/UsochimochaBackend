package com.app.usochicamochabackend.user.application.dto;

public record RestoreUserRequest(
        Long userId,
        boolean restoreWithNewPassword,
        String newPassword
) {
}
