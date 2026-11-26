package ru.ifmo.se.application.exception;

import java.io.Serial;

public class SnapshotUploadException extends StorageException {
    @Serial
    private static final long serialVersionUID = 1L;

    public SnapshotUploadException(Throwable cause) {
        super("Failed to upload file snapshot to S3 Garage", cause);
    }

    @Override
    public String getLocalizedMessage() {
        return "Не удалось загрузить снимок файла в S3 Garage";
    }
}
