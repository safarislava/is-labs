package ru.ifmo.se.presentation.rest.advice;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import ru.ifmo.se.application.exception.StorageException;
import ru.ifmo.se.presentation.dto.response.ErrorResponse;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import javax.transaction.xa.XAException;

public final class DatabaseExceptionResolver {

    private static final String SERIALIZATION_FAILURE = "40001";
    private static final String DEADLOCK_DETECTED = "40P01";

    private DatabaseExceptionResolver() {
    }

    public static Response resolve(Throwable exception) {
        List<Throwable> chain = collectThrowables(exception);

        StorageException storageException = findCause(chain, StorageException.class);
        if (storageException != null) {
            return response(
                    Response.Status.SERVICE_UNAVAILABLE,
                    storageException.getLocalizedMessage()
            );
        }

        String state = findSqlState(chain);
        if (!state.isEmpty()) {
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

        if (findCause(chain, XAException.class) != null) {
            return response(
                    Response.Status.INTERNAL_SERVER_ERROR,
                    "Ошибка распределенной транзакции (2PC): изменения отменены"
            );
        }

        return response(
                Response.Status.BAD_REQUEST,
                "Нарушение уникальности данных"
        );
    }

    private static Response response(Response.Status status, String message) {
        return Response.status(status)
                .entity(new ErrorResponse(message, status.getStatusCode()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static List<Throwable> collectThrowables(Throwable root) {
        List<Throwable> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<Throwable> queue = new ArrayDeque<>();
        visited.add(root);
        queue.add(root);
        while (!queue.isEmpty()) {
            Throwable current = queue.removeFirst();
            result.add(current);
            Throwable cause = current.getCause();
            if (cause != null && visited.add(cause)) {
                queue.addLast(cause);
            }
            for (Throwable suppressed : current.getSuppressed()) {
                if (suppressed != null && visited.add(suppressed)) {
                    queue.addLast(suppressed);
                }
            }
        }
        return result;
    }

    private static <T extends Throwable> T findCause(List<Throwable> chain, Class<T> targetClass) {
        for (Throwable current : chain) {
            if (targetClass.isInstance(current)) {
                return targetClass.cast(current);
            }
        }
        return null;
    }

    private static String findSqlState(List<Throwable> chain) {
        for (Throwable current : chain) {
            if (current instanceof SQLException sqlEx && sqlEx.getSQLState() != null) {
                return sqlEx.getSQLState();
            }
        }
        return "";
    }
}
