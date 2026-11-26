package ru.ifmo.se.application.exception;

import java.io.Serial;

public class InvalidImportJsonException extends InvalidImportFileException {
    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidImportJsonException(Throwable cause) {
        super("Invalid JSON format in import file", cause);
    }

    @Override
    public String getLocalizedMessage() {
        return "Некорректный формат JSON в файле импорта";
    }
}
