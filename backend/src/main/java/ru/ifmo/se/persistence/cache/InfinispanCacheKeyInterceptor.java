package ru.ifmo.se.persistence.cache;

import org.eclipse.persistence.internal.identitymaps.CacheKey;
import org.eclipse.persistence.sessions.interceptors.CacheKeyInterceptor;

import java.io.Serial;

@SuppressWarnings("PMD.UselessOverridingMethod")
final class InfinispanCacheKeyInterceptor extends CacheKeyInterceptor {
    InfinispanCacheKeyInterceptor(CacheKey wrappedKey) {
        super(wrappedKey);
    }
}
