package ru.ifmo.se.application.interceptor;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ru.ifmo.se.application.annotation.Serializable;

import java.io.Serial;

@Interceptor
@Serializable
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 210)
public class SerializableInterceptor implements java.io.Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @PersistenceContext
    private transient EntityManager entityManager;

    @AroundInvoke
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public Object applySerializable(InvocationContext context) throws Exception {
        entityManager.createNativeQuery("SET TRANSACTION ISOLATION LEVEL SERIALIZABLE").executeUpdate();
        return context.proceed();
    }
}
