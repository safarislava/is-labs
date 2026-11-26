package ru.ifmo.se.application.exception;

import java.io.Serial;

public abstract class ResourceNotFoundException extends ApplicationException {
    @Serial
    private static final long serialVersionUID = 1L;

    protected ResourceNotFoundException(String message) {
        super(ErrorType.NOT_FOUND, message);
    }

    protected ResourceNotFoundException(String message, Throwable cause) {
        super(ErrorType.NOT_FOUND, message, cause);
    }
}
