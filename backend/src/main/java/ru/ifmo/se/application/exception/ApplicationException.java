package ru.ifmo.se.application.exception;

import lombok.Getter;

import java.io.Serial;

@Getter
public abstract class ApplicationException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final ErrorType errorType;

    protected ApplicationException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    protected ApplicationException(ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public enum ErrorType {
        INVALID_INPUT,
        UNAUTHORIZED,
        FORBIDDEN,
        NOT_FOUND,
        CONFLICT,
        SERVICE_UNAVAILABLE
    }
}
