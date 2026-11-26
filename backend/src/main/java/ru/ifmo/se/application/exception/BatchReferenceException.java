package ru.ifmo.se.application.exception;

import java.io.Serial;

public abstract class BatchReferenceException extends InvalidInputException {
    @Serial
    private static final long serialVersionUID = 1L;

    protected BatchReferenceException(String message) {
        super(message);
    }
}
