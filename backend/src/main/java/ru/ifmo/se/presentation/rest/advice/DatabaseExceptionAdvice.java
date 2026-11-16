package ru.ifmo.se.presentation.rest.advice;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.persistence.exceptions.DatabaseException;

@Provider
public class DatabaseExceptionAdvice implements ExceptionMapper<DatabaseException> {

    @Override
    public Response toResponse(DatabaseException exception) {
        return DatabaseExceptionResolver.resolve(exception);
    }
}
