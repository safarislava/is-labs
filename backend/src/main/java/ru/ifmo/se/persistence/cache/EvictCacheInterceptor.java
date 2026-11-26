package ru.ifmo.se.persistence.cache;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.infinispan.manager.DefaultCacheManager;

import java.io.Serial;
import java.io.Serializable;

@Interceptor
@EvictCache
@Priority(Interceptor.Priority.APPLICATION)
public class EvictCacheInterceptor implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @PersistenceContext
    private transient EntityManager entityManager;

    @AroundInvoke
    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.CloseResource"})
    public Object evict(InvocationContext context) throws Exception {
        Object result = context.proceed();

        EvictCache annotation = context.getMethod().getAnnotation(EvictCache.class);
        Class<?>[] entityClasses = annotation != null ? annotation.value() : new Class<?>[0];

        EntityManagerFactory factory = entityManager.getEntityManagerFactory();
        Cache jpaCache = factory.getCache();
        DefaultCacheManager cacheManager = (DefaultCacheManager) factory.unwrap(AbstractSession.class)
                .getRootSession(null)
                .getProperty(DefaultCacheManager.class.getName());

        if (entityClasses.length == 0) {
            jpaCache.evictAll();
            cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        } else {
            for (Class<?> entityClass : entityClasses) {
                jpaCache.evict(entityClass);
                if (cacheManager.cacheExists(entityClass.getName())) {
                    cacheManager.getCache(entityClass.getName()).clear();
                }
            }
        }

        entityManager.clear();
        return result;
    }
}
