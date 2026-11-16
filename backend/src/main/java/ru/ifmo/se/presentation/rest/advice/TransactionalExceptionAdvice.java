package ru.ifmo.se.presentation.rest.advice;

import jakarta.transaction.TransactionalException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TransactionalExceptionAdvice implements ExceptionMapper<TransactionalException> {

    @Override
    public Response toResponse(TransactionalException exception) {
        return DatabaseExceptionResolver.resolve(exception);
    }
}
