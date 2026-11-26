package ru.ifmo.se.application.exception;

import java.io.Serial;

public class EmptySnapshotContentException extends StorageException {
    @Serial
    private static final long serialVersionUID = 1L;

    public EmptySnapshotContentException() {
        super("File content is missing for S3 Garage snapshot");
    }

    @Override
    public String getLocalizedMessage() {
        return "Отсутствует содержимое файла для сохранения снимка в S3 Garage";
    }
}
