package ru.ifmo.se.persistence.cache;

import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.sessions.SessionCustomizer;

public class InfinispanCacheSessionCustomizer implements SessionCustomizer {

    @Override
    public void customize(Session session) {
        for (ClassDescriptor descriptor : session.getDescriptors().values()) {
            if (!descriptor.isAggregateDescriptor()) {
                descriptor.getCachePolicy().setCacheInterceptorClass(InfinispanCacheInterceptor.class);
            }
        }
    }
}
