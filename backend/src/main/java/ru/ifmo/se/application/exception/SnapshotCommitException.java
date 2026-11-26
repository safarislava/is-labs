package ru.ifmo.se.application.exception;

import java.io.Serial;

public class SnapshotCommitException extends StorageException {
    @Serial
    private static final long serialVersionUID = 1L;

    public SnapshotCommitException(Throwable cause) {
        super("Failed to commit snapshot in S3 Garage during commit phase", cause);
    }

    @Override
    public String getLocalizedMessage() {
        return "Ошибка фиксации снимка в S3 Garage на фазе commit (2PC)";
    }
}
