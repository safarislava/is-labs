package ru.ifmo.se.application.interceptor;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ru.ifmo.se.application.annotation.RepeatableReadTransactional;

import java.io.Serial;
import java.io.Serializable;

@Interceptor
@RepeatableReadTransactional
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 210)
public class RepeatableReadTransactionalInterceptor implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @PersistenceContext
    private transient EntityManager entityManager;

    @AroundInvoke
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public Object applyRepeatableRead(InvocationContext context) throws Exception {
        entityManager.createNativeQuery("SET TRANSACTION ISOLATION LEVEL REPEATABLE READ").executeUpdate();
        return context.proceed();
    }
}
