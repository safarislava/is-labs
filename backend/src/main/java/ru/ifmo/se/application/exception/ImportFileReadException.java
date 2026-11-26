package ru.ifmo.se.application.exception;

import java.io.Serial;

public class ImportFileReadException extends InvalidImportFileException {
    @Serial
    private static final long serialVersionUID = 1L;

    public ImportFileReadException(Throwable cause) {
        super("Failed to read import file", cause);
    }

    @Override
    public String getLocalizedMessage() {
        return "Ошибка чтения файла импорта";
    }
}
