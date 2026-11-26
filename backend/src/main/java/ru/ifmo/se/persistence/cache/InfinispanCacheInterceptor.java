package ru.ifmo.se.persistence.cache;

import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.internal.identitymaps.CacheKey;
import org.eclipse.persistence.internal.identitymaps.IdentityMap;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.sessions.interceptors.CacheInterceptor;
import org.eclipse.persistence.sessions.interceptors.CacheKeyInterceptor;

import java.util.Map;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.UselessOverridingMethod"})
public final class InfinispanCacheInterceptor extends CacheInterceptor {
    InfinispanCacheInterceptor(IdentityMap targetIdentityMap, AbstractSession interceptedSession) {
        super(targetIdentityMap, interceptedSession);
    }

    @Override
    public Object clone() {
        return null;
    }

    @Override
    protected CacheKeyInterceptor createCacheKeyInterceptor(CacheKey wrappedCacheKey) {
        return new InfinispanCacheKeyInterceptor(wrappedCacheKey);
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
