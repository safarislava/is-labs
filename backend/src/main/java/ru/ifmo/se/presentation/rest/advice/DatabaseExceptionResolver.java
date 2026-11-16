package ru.ifmo.se.presentation.rest.advice;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ru.ifmo.se.presentation.dto.response.ErrorResponse;

import java.sql.SQLException;

public final class DatabaseExceptionResolver {

    private static final String SERIALIZATION_FAILURE = "40001";
    private static final String DEADLOCK_DETECTED = "40P01";

    private DatabaseExceptionResolver() {
    }

    public static Response resolve(Throwable exception) {
        String state = findSqlState(exception);
        return switch (state) {
            case SERIALIZATION_FAILURE, DEADLOCK_DETECTED -> response(
                    Response.Status.CONFLICT,
                    "Конфликт параллельного изменения данных: объект был изменен другим пользователем"
            );
            default -> response(
                    Response.Status.BAD_REQUEST,
                    "Нарушение уникальности данных"
            );
        };
    }

    private static Response response(Response.Status status, String message) {
        return Response.status(status)
                .entity(new ErrorResponse(message, status.getStatusCode()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static String findSqlState(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlEx && sqlEx.getSQLState() != null) {
                return sqlEx.getSQLState();
            }
            current = current.getCause();
        }
        return "";
    }
}
