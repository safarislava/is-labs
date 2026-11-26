package ru.ifmo.se.application.exception;

import java.io.Serial;

public class MissingImportFileException extends InvalidImportFileException {
    @Serial
    private static final long serialVersionUID = 1L;

    public MissingImportFileException() {
        super("Import file was not provided");
    }

    @Override
    public String getLocalizedMessage() {
        return "Файл импорта не передан";
    }
}
