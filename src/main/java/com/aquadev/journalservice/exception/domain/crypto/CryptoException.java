package com.aquadev.journalservice.exception.domain.crypto;

import com.aquadev.journalservice.exception.base.InternalServerErrorException;

public class CryptoException extends InternalServerErrorException {

    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
