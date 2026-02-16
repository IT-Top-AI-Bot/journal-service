package com.aquadev.ittopai.exception.domain.user;

import com.aquadev.ittopai.exception.base.ConflictException;

public class UserAlreadyExistException extends ConflictException {
    public UserAlreadyExistException(String message) {
        super(message);
    }
}
