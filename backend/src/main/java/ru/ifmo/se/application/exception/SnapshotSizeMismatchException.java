package ru.ifmo.se.application.exception;

import java.io.Serial;

public class SnapshotSizeMismatchException extends StorageException {
    @Serial
    private static final long serialVersionUID = 1L;

    public SnapshotSizeMismatchException() {
        super("Staging snapshot size mismatch in S3 Garage");
    }

    @Override
    public String getLocalizedMessage() {
        return "Размер временного снимка в S3 Garage не совпадает с ожидаемым";
    }
}
