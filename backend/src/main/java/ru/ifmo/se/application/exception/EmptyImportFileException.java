package ru.ifmo.se.application.exception;

import java.io.Serial;

public class EmptyImportFileException extends InvalidImportFileException {
    @Serial
    private static final long serialVersionUID = 1L;

    public EmptyImportFileException() {
        super("Import file cannot be empty");
    }

    @Override
    public String getLocalizedMessage() {
        return "Файл импорта не может быть пустым";
    }
}
