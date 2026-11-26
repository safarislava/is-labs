package ru.ifmo.se.persistence.cache;

import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.internal.identitymaps.CacheKey;
import org.eclipse.persistence.internal.identitymaps.IdentityMap;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.sessions.interceptors.CacheInterceptor;
import org.eclipse.persistence.sessions.interceptors.CacheKeyInterceptor;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;

import java.util.Map;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.UselessOverridingMethod"})
public final class InfinispanCacheInterceptor extends CacheInterceptor {

    private final Cache<Object, Object> cache;

    @SuppressWarnings("PMD.CloseResource")
    public InfinispanCacheInterceptor(IdentityMap targetIdentityMap, AbstractSession interceptedSession) {
        super(targetIdentityMap, interceptedSession);
        Object property = interceptedSession.getRootSession(null).getProperty(DefaultCacheManager.class.getName());
        if (!(property instanceof DefaultCacheManager manager)) {
            throw new IllegalStateException("DefaultCacheManager is not configured on session");
        }
        this.cache = manager.getCache(targetIdentityMap.getDescriptorClass().getName());
    }

    @Override
    public Object clone() {
        return null;
    }

    @Override
    protected CacheKeyInterceptor createCacheKeyInterceptor(CacheKey wrappedCacheKey) {
        return new InfinispanCacheKeyInterceptor(wrappedCacheKey, cache);
    }

    @Override
    public Map<Object, Object> getAllFromIdentityMapWithEntityPK(Object[] pkList, ClassDescriptor descriptor, AbstractSession session) {
        return Map.of();
    }

    @Override
    public Map<Object, CacheKey> getAllCacheKeysFromIdentityMapWithEntityPK(Object[] pkList, ClassDescriptor descriptor, AbstractSession session) {
        return Map.of();
    }

    @Override
    public void release() {

    }
}
