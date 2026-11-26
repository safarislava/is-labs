package ru.ifmo.se.application.exception;

import java.io.Serial;

public abstract class StorageException extends ApplicationException {
    @Serial
    private static final long serialVersionUID = 1L;

    protected StorageException(String message) {
        super(ErrorType.SERVICE_UNAVAILABLE, message);
    }

    protected StorageException(String message, Throwable cause) {
        super(ErrorType.SERVICE_UNAVAILABLE, message, cause);
    }
}
