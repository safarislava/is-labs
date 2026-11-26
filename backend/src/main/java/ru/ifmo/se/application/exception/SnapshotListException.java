package ru.ifmo.se.application.exception;

import java.io.Serial;

public class SnapshotListException extends StorageException {
    @Serial
    private static final long serialVersionUID = 1L;

    public SnapshotListException(Throwable cause) {
        super("Failed to list objects in S3 Garage", cause);
    }

    @Override
    public String getLocalizedMessage() {
        return "Не удалось получить список объектов в S3 Garage";
    }
}
