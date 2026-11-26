package ru.ifmo.se.application.exception;

import java.io.Serial;

public abstract class ConflictException extends ApplicationException {
    @Serial
    private static final long serialVersionUID = 1L;

    protected ConflictException(String message) {
        super(ErrorType.CONFLICT, message);
    }
}
