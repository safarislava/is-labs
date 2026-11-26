package ru.ifmo.se.persistence.cache;

import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.sessions.SessionCustomizer;
import org.eclipse.persistence.sessions.SessionEvent;
import org.eclipse.persistence.sessions.SessionEventAdapter;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("PMD.CloseResource")
public class InfinispanCacheSessionCustomizer implements SessionCustomizer {

    private static final long MAX_CACHE_ENTRIES = 10_000L;
    private static final long CACHE_LIFESPAN_MINUTES = 30L;
    private static final long CACHE_MAX_IDLE_MINUTES = 10L;

    @Override
    public void customize(Session session) {
        GlobalConfiguration globalConfig = new GlobalConfigurationBuilder()
                .nonClusteredDefault()
                .build();
        DefaultCacheManager cacheManager = new DefaultCacheManager(globalConfig);
        session.setProperty(DefaultCacheManager.class.getName(), cacheManager);
        session.setProperty(L2CacheLogger.class.getName(), new L2CacheLogger());
        session.getEventManager().addListener(new CacheShutdownListener(cacheManager));

        Configuration cacheConfig = new ConfigurationBuilder()
                .memory()
                    .storage(StorageType.HEAP)
                    .maxCount(MAX_CACHE_ENTRIES)
                .expiration()
                    .lifespan(CACHE_LIFESPAN_MINUTES, TimeUnit.MINUTES)
                    .maxIdle(CACHE_MAX_IDLE_MINUTES, TimeUnit.MINUTES)
                .statistics()
                    .enable()
                .build();

        for (ClassDescriptor descriptor : session.getDescriptors().values()) {
            if (!descriptor.isAggregateDescriptor() && descriptor.getJavaClass() != null) {
                cacheManager.defineConfiguration(descriptor.getJavaClass().getName(), cacheConfig);
                descriptor.getCachePolicy().setCacheInterceptorClass(InfinispanCacheInterceptor.class);
            }
        }
    }
}

@SuppressWarnings("PMD.CloseResource")
final class CacheShutdownListener extends SessionEventAdapter {

    private final DefaultCacheManager cacheManager;

    CacheShutdownListener(DefaultCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void postLogout(SessionEvent event) {
        cacheManager.stop();
    }
}
