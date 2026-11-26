package ru.ifmo.se.application.exception;

import java.io.Serial;

public class ImportOperationAccessDeniedException extends AccessDeniedException {
    @Serial
    private static final long serialVersionUID = 1L;

    public ImportOperationAccessDeniedException(int operationId) {
        super(String.format("Access denied to import operation #%d file", operationId));
    }

    @Override
    public String getLocalizedMessage() {
        return "Нет доступа к файлу этой операции импорта";
    }
}
