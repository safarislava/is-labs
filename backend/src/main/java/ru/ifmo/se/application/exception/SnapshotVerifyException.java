package ru.ifmo.se.application.exception;

import java.io.Serial;

public class SnapshotVerifyException extends StorageException {
    @Serial
    private static final long serialVersionUID = 1L;

    public SnapshotVerifyException(Throwable cause) {
        super("Failed to verify snapshot in S3 Garage during prepare phase", cause);
    }

    @Override
    public String getLocalizedMessage() {
        return "Ошибка проверки снимка в S3 Garage на фазе prepare (2PC)";
    }
}
