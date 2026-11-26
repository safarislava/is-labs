package ru.ifmo.se.persistence.cache;

import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.internal.identitymaps.CacheKey;
import org.eclipse.persistence.internal.identitymaps.IdentityMap;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.sessions.interceptors.CacheInterceptor;
import org.eclipse.persistence.sessions.interceptors.CacheKeyInterceptor;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.UselessOverridingMethod"})
public final class InfinispanCacheInterceptor extends CacheInterceptor {
    private final Cache<Object, Object> cache;
    private final L2CacheLogger statistics;

    @SuppressWarnings("PMD.CloseResource")
    public InfinispanCacheInterceptor(IdentityMap targetIdentityMap, AbstractSession interceptedSession) {
        super(targetIdentityMap, interceptedSession);
        AbstractSession rootSession = interceptedSession.getRootSession(null);
        DefaultCacheManager manager = (DefaultCacheManager) rootSession.getProperty(DefaultCacheManager.class.getName());
        this.cache = manager.getCache(targetIdentityMap.getDescriptorClass().getName());
        this.statistics = (L2CacheLogger) rootSession.getProperty(L2CacheLogger.class.getName());
    }

    @Override
    protected CacheKeyInterceptor createCacheKeyInterceptor(CacheKey wrappedCacheKey) {
        return new InfinispanCacheKeyInterceptor(wrappedCacheKey, cache);
    }

    @Override
    public Object get(Object primaryKey) {
        Object localEntity = super.get(primaryKey);
        if (localEntity != null) {
            return statistics.recordValue(targetIdentityMap.getDescriptorClass(), primaryKey, localEntity);
        }

        Object infinispanEntity = primaryKey != null ? cache.get(primaryKey) : null;
        if (infinispanEntity != null) {
            super.put(primaryKey, infinispanEntity, null, System.currentTimeMillis());
        }
        return statistics.recordValue(targetIdentityMap.getDescriptorClass(), primaryKey, infinispanEntity);
    }

    @Override
    public CacheKey put(Object key, Object value, Object writeLockValue, long readTime) {
        CacheKey cacheKey = super.put(key, value, writeLockValue, readTime);
        if (key != null && value != null) {
            cache.put(key, value);
        }
        return cacheKey;
    }

    @Override
    public boolean containsKey(Object primaryKey) {
        return super.containsKey(primaryKey) || (primaryKey != null && cache.containsKey(primaryKey));
    }

    @Override
    public CacheKey getCacheKey(Object primaryKey, boolean forCreate) {
        CacheKey cacheKey = super.getCacheKey(primaryKey, forCreate);
        if (cacheKey == null && !forCreate && primaryKey != null) {
            Object cached = cache.get(primaryKey);
            if (cached != null) {
                cacheKey = super.put(primaryKey, cached, null, System.currentTimeMillis());
            }
        }
        if (!forCreate && primaryKey != null) {
            statistics.recordKey(targetIdentityMap.getDescriptorClass(), primaryKey, cacheKey);
        }
        return cacheKey != null ? createCacheKeyInterceptor(cacheKey) : null;
    }

    @Override
    public CacheKey getCacheKeyForLock(Object primaryKey) {
        CacheKey cacheKey = super.getCacheKeyForLock(primaryKey);
        if (cacheKey == null && primaryKey != null) {
            Object cached = cache.get(primaryKey);
            if (cached != null) {
                cacheKey = super.put(primaryKey, cached, null, System.currentTimeMillis());
            }
        }
        return cacheKey != null ? createCacheKeyInterceptor(cacheKey) : null;
    }

    @Override
    public Object remove(Object key, Object object) {
        if (key != null) {
            cache.remove(key);
        }
        return super.remove(key, object);
    }

    @Override
    public Object remove(CacheKey key) {
        if (key != null && key.getKey() != null) {
            cache.remove(key.getKey());
        }
        return super.remove(key);
    }

    @Override
    public Enumeration<CacheKey> keys(boolean checkReadLocks) {
        Enumeration<CacheKey> keys = super.keys(checkReadLocks);
        List<CacheKey> wrappedKeys = new ArrayList<>();
        while (keys.hasMoreElements()) {
            CacheKey cacheKey = keys.nextElement();
            if (cacheKey != null) {
                wrappedKeys.add(createCacheKeyInterceptor(cacheKey));
            }
        }
        return Collections.enumeration(wrappedKeys);
    }

    @Override
    public Map<Object, Object> getAllFromIdentityMapWithEntityPK(
            Object[] pkList, ClassDescriptor descriptor, AbstractSession session) {
        if (pkList == null || pkList.length == 0) {
            return Collections.emptyMap();
        }
        Map<Object, Object> result = new HashMap<>(pkList.length);
        for (Object primaryKey : pkList) {
            if (primaryKey == null) {
                continue;
            }
            Object cached = get(primaryKey);
            if (cached != null) {
                result.put(primaryKey, cached);
            }
        }
        return result;
    }

    @Override
    public Map<Object, CacheKey> getAllCacheKeysFromIdentityMapWithEntityPK(
            Object[] pkList, ClassDescriptor descriptor, AbstractSession session) {
        if (pkList == null || pkList.length == 0) {
            return Collections.emptyMap();
        }
        Map<Object, CacheKey> result = new HashMap<>(pkList.length);
        for (Object primaryKey : pkList) {
            if (primaryKey == null) {
                continue;
            }
            CacheKey cached = getCacheKey(primaryKey, false);
            if (cached != null) {
                result.put(primaryKey, cached);
            }
        }
        return result;
    }

    @Override
    public void release() {
        cache.clear();
        targetIdentityMap.release();
    }

    @Override
    @SuppressWarnings("PMD.ProperCloneImplementation")
    public InfinispanCacheInterceptor clone() {
        return new InfinispanCacheInterceptor((IdentityMap) targetIdentityMap.clone(), interceptedSession);
    }
}
