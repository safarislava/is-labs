package ru.ifmo.se.application.interceptor;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import ru.ifmo.se.application.annotation.LogCache;
import ru.ifmo.se.persistence.cache.L2CacheLogger;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.logging.Logger;

@Interceptor
@LogCache
@Priority(Interceptor.Priority.APPLICATION + 10)
public class LogCacheInterceptor implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(LogCacheInterceptor.class.getName());

    @PersistenceContext
    private transient EntityManager entityManager;

    @AroundInvoke
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    public Object logCacheStatistics(InvocationContext context) throws Exception {
        L2CacheLogger statistics = resolveStatistics();
        if (statistics == null || statistics.isTracking()) {
            return context.proceed();
        }

        statistics.beginTracking();
        try {
            return context.proceed();
        } finally {
            Map<String, String> accesses = statistics.finishTracking();
            for (Map.Entry<String, String> entry : accesses.entrySet()) {
                LOGGER.info(() -> String.format("[L2 Cache] %s: %s", entry.getKey(), entry.getValue()));
            }
        }
    }

    private L2CacheLogger resolveStatistics() {
        AbstractSession session = entityManager.getEntityManagerFactory().unwrap(AbstractSession.class);
        return (L2CacheLogger) session.getRootSession(null).getProperty(L2CacheLogger.class.getName());
    }
}
