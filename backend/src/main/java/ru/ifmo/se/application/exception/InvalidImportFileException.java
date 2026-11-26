package ru.ifmo.se.application.exception;

import java.io.Serial;

public abstract class InvalidImportFileException extends InvalidInputException {
    @Serial
    private static final long serialVersionUID = 1L;

    protected InvalidImportFileException(String message) {
        super(message);
    }

    protected InvalidImportFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
