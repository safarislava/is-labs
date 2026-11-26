package ru.ifmo.se.application.exception;

import java.io.Serial;

public class SnapshotDownloadException extends StorageException {
    @Serial
    private static final long serialVersionUID = 1L;

    public SnapshotDownloadException(Throwable cause) {
        super("Failed to download file snapshot from S3 Garage", cause);
    }

    @Override
    public String getLocalizedMessage() {
        return "Не удалось получить снимок файла из S3 Garage";
    }
}
