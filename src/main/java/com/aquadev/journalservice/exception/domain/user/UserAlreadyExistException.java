package com.aquadev.journalservice.exception.domain.user;

import com.aquadev.journalservice.exception.base.ConflictException;

public class UserAlreadyExistException extends ConflictException {
    public UserAlreadyExistException(String message) {
        super(message);
    }
}
