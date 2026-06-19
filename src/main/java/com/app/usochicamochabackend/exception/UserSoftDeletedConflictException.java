package com.app.usochicamochabackend.exception;

import com.app.usochicamochabackend.user.application.dto.UserResponse;
import lombok.Getter;

@Getter
public class UserSoftDeletedConflictException extends RuntimeException {
    private final UserResponse softDeletedUser;
    private final String suggestedAction;

    public UserSoftDeletedConflictException(String message, UserResponse softDeletedUser) {
        super(message);
        this.softDeletedUser = softDeletedUser;
        this.suggestedAction = "restore_or_create_new";
    }

    public UserSoftDeletedConflictException(String message, UserResponse softDeletedUser, String suggestedAction) {
        super(message);
        this.softDeletedUser = softDeletedUser;
        this.suggestedAction = suggestedAction;
    }
}
