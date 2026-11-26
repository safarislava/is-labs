package ru.ifmo.se.application.exception;

import java.io.Serial;

public abstract class UnauthorizedException extends ApplicationException {
    @Serial
    private static final long serialVersionUID = 1L;

    protected UnauthorizedException(String message) {
        super(ErrorType.UNAUTHORIZED, message);
    }

    protected UnauthorizedException(String message, Throwable cause) {
        super(ErrorType.UNAUTHORIZED, message, cause);
    }
}
