package ru.ifmo.se.application.exception;

import java.io.Serial;

public class SnapshotEtagMismatchException extends StorageException {
    @Serial
    private static final long serialVersionUID = 1L;

    public SnapshotEtagMismatchException() {
        super("Staging snapshot ETag mismatch in S3 Garage");
    }

    @Override
    public String getLocalizedMessage() {
        return "Контрольная сумма ETag снимка в S3 Garage не совпадает";
    }
}
