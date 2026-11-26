package ru.ifmo.se.application.exception;

import java.io.Serial;

public class EmptyImportJsonException extends InvalidImportFileException {
    @Serial
    private static final long serialVersionUID = 1L;

    public EmptyImportJsonException() {
        super("Import JSON file is empty");
    }

    @Override
    public String getLocalizedMessage() {
        return "JSON файл импорта пуст";
    }
}
