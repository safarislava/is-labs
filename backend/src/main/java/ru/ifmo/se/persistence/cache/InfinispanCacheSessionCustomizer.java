package ru.ifmo.se.persistence.cache;

import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.sessions.SessionCustomizer;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

@SuppressWarnings("PMD.CloseResource")
public class InfinispanCacheSessionCustomizer implements SessionCustomizer {

    @Override
    public void customize(Session session) {
        GlobalConfiguration globalConfig = new GlobalConfigurationBuilder()
                .nonClusteredDefault()
                .build();
        DefaultCacheManager cacheManager = new DefaultCacheManager(globalConfig);
        session.setProperty(DefaultCacheManager.class.getName(), cacheManager);

        for (ClassDescriptor descriptor : session.getDescriptors().values()) {
            if (!descriptor.isAggregateDescriptor()) {
                descriptor.getCachePolicy().setCacheInterceptorClass(InfinispanCacheInterceptor.class);
            }
        }
    }
}
