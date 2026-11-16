package ru.ifmo.se.presentation.rest.advice;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.persistence.exceptions.DatabaseException;
import ru.ifmo.se.presentation.dto.response.ErrorResponse;

@Provider
public class DatabaseExceptionAdvice implements ExceptionMapper<DatabaseException> {

    @Override
    public Response toResponse(DatabaseException exception) {
        ErrorResponse errorResponse = new ErrorResponse(
                "Нарушение уникальности данных",
                Response.Status.BAD_REQUEST.getStatusCode()
        );
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
