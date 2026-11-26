package ru.ifmo.se.application.exception;

import java.io.Serial;

public class ImportOperationNotFoundException extends ResourceNotFoundException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int operationId;

    public ImportOperationNotFoundException(int operationId) {
        super(String.format("Import operation not found: %d", operationId));
        this.operationId = operationId;
    }

    @Override
    public String getLocalizedMessage() {
        return String.format("Операция импорта #%d не найдена", operationId);
    }
}
