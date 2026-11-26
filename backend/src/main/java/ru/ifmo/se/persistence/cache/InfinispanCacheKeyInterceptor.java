package ru.ifmo.se.persistence.cache;

import org.eclipse.persistence.internal.identitymaps.CacheKey;
import org.eclipse.persistence.sessions.interceptors.CacheKeyInterceptor;
import org.infinispan.Cache;

import java.io.Serial;

@SuppressWarnings({"PMD.UselessOverridingMethod", "PMD.UnusedPrivateField"})
final class InfinispanCacheKeyInterceptor extends CacheKeyInterceptor {
    @Serial
    private static final long serialVersionUID = 1L;
    private final transient Cache<Object, Object> cache;

    InfinispanCacheKeyInterceptor(CacheKey wrappedKey, Cache<Object, Object> cache) {
        super(wrappedKey);
        this.cache = cache;
        wrappedKey.setIsWrapper(true);
    }

    @Override
    public Object getObject() {
        Object cached = super.getObject();
        Object key = getKey();
        if (cached != null || key == null) {
            return cached;
        }
        cached = cache.get(key);
        if (cached != null) {
            super.setObject(cached);
        }
        return cached;
    }

    @Override
    public void setObject(Object object) {
        super.setObject(object);
        Object key = getKey();
        if (object != null) {
            cache.put(key, object);
        } else {
            cache.remove(key);
        }
    }

    @Override
    public void setInvalidationState(int invalidationState) {
        super.setInvalidationState(invalidationState);
        Object key = getKey();
        if (invalidationState == CacheKey.CACHE_KEY_INVALID && key != null) {
            cache.remove(key);
        }
    }

    @Override
    public Object removeFromOwningMap() {
        Object key = getKey();
        if (key != null) {
            cache.remove(key);
        }
        return super.removeFromOwningMap();
    }
}
