package ru.ifmo.se.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ValidationException;
import ru.ifmo.se.application.exception.ApplicationException;
import ru.ifmo.se.persistence.entity.ImportStatus;

import java.sql.SQLException;

@ApplicationScoped
public class ImportFailureClassifier {
    private static final int MAX_DEPTH = 10;

    public ImportStatus classify(Throwable exception) {
        return isInvalidFile(exception, 0) ? ImportStatus.INVALID_FILE : ImportStatus.FAILED;
    }

    private boolean isInvalidFile(Throwable throwable, int depth) {
        if (throwable == null || depth > MAX_DEPTH) {
            return false;
        }
        if (isDirectInvalidFile(throwable) || isInvalidFile(throwable.getCause(), depth + 1)) {
            return true;
        }
        for (Throwable suppressed : throwable.getSuppressed()) {
            if (isInvalidFile(suppressed, depth + 1)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDirectInvalidFile(Throwable throwable) {
        if (throwable instanceof ApplicationException appEx) {
            return switch (appEx.getErrorType()) {
                case INVALID_INPUT, NOT_FOUND, CONFLICT -> true;
                default -> false;
            };
        }
        if (throwable instanceof ValidationException) {
            return true;
        }
        if (throwable instanceof SQLException sqlEx) {
            String state = sqlEx.getSQLState();
            return state != null && (state.startsWith("22") || state.startsWith("23"));
        }
        return false;
    }
}
