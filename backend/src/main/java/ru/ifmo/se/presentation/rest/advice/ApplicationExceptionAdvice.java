package ru.ifmo.se.presentation.rest.advice;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ru.ifmo.se.application.exception.ApplicationException;
import ru.ifmo.se.presentation.dto.response.ErrorResponse;

@Provider
public class ApplicationExceptionAdvice implements ExceptionMapper<ApplicationException> {

    @Override
    public Response toResponse(ApplicationException exception) {
        Response.Status status = mapStatus(exception.getErrorType());
        ErrorResponse errorResponse = new ErrorResponse(
                exception.getLocalizedMessage(),
                status.getStatusCode()
        );
        return Response.status(status)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static Response.Status mapStatus(ApplicationException.ErrorType errorType) {
        return switch (errorType) {
            case INVALID_INPUT -> Response.Status.BAD_REQUEST;
            case UNAUTHORIZED -> Response.Status.UNAUTHORIZED;
            case FORBIDDEN -> Response.Status.FORBIDDEN;
            case NOT_FOUND -> Response.Status.NOT_FOUND;
            case CONFLICT -> Response.Status.CONFLICT;
            case SERVICE_UNAVAILABLE -> Response.Status.SERVICE_UNAVAILABLE;
        };
    }
}
