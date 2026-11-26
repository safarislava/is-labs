package ru.ifmo.se.application.exception;

import java.io.Serial;

public abstract class InvalidInputException extends ApplicationException {
    @Serial
    private static final long serialVersionUID = 1L;

    protected InvalidInputException(String message) {
        super(ErrorType.INVALID_INPUT, message);
    }

    protected InvalidInputException(String message, Throwable cause) {
        super(ErrorType.INVALID_INPUT, message, cause);
    }
}
