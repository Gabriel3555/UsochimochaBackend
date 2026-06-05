package com.app.usochicamochabackend.exception;

import com.app.usochicamochabackend.user.application.dto.UserResponse;
import lombok.Getter;

@Getter
public class UserSoftDeletedConflictException extends RuntimeException {
    private final UserResponse softDeletedUser;

    public UserSoftDeletedConflictException(String message, UserResponse softDeletedUser) {
        super(message);
        this.softDeletedUser = softDeletedUser;
    }
}
