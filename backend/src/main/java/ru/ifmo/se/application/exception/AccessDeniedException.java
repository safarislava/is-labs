package ru.ifmo.se.application.exception;

import java.io.Serial;

public abstract class AccessDeniedException extends ApplicationException {
    @Serial
    private static final long serialVersionUID = 1L;

    protected AccessDeniedException(String message) {
        super(ErrorType.FORBIDDEN, message);
    }

    protected AccessDeniedException(String message, Throwable cause) {
        super(ErrorType.FORBIDDEN, message, cause);
    }
}
