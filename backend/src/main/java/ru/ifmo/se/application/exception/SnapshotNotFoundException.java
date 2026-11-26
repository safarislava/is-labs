package ru.ifmo.se.application.exception;

import java.io.Serial;

public class SnapshotNotFoundException extends ResourceNotFoundException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String identifier;
    private final boolean byOperationId;

    public SnapshotNotFoundException(int operationId) {
        super(String.format("Snapshot not found for import operation: %d", operationId));
        this.identifier = String.valueOf(operationId);
        this.byOperationId = true;
    }

    public SnapshotNotFoundException(String key) {
        super(String.format("Snapshot not found in S3: %s", key));
        this.identifier = key;
        this.byOperationId = false;
    }

    public SnapshotNotFoundException(String key, Throwable cause) {
        super(String.format("Snapshot not found in S3: %s", key), cause);
        this.identifier = key;
        this.byOperationId = false;
    }

    @Override
    public String getLocalizedMessage() {
        if (byOperationId) {
            return String.format("Снимок файла для операции импорта #%s не найден", identifier);
        }
        return String.format("Снимок файла '%s' не найден в хранилище S3", identifier);
    }
}
