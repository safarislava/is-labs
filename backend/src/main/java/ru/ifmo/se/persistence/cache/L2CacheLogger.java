package ru.ifmo.se.persistence.cache;

import lombok.Getter;
import org.eclipse.persistence.internal.identitymaps.CacheKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class L2CacheLogger {
    private final ThreadLocal<List<String>> currentAccesses = new ThreadLocal<>();

    public void beginTracking() {
        currentAccesses.set(new ArrayList<>());
    }

    public boolean isTracking() {
        return currentAccesses.get() != null;
    }

    public <T> T recordValue(Class<?> entityClass, Object primaryKey, T value) {
        record(entityClass, primaryKey, value != null ? CacheOutcome.HIT : CacheOutcome.MISS);
        return value;
    }

    public void recordKey(Class<?> entityClass, Object primaryKey, CacheKey cacheKey) {
        boolean hit = cacheKey != null
                && cacheKey.getInvalidationState() != CacheKey.CACHE_KEY_INVALID
                && cacheKey.getObject() != null;
        record(entityClass, primaryKey, hit ? CacheOutcome.HIT : CacheOutcome.MISS);
    }

    public List<String> finishTracking() {
        List<String> accesses = currentAccesses.get();
        currentAccesses.remove();
        return accesses != null ? accesses : Collections.emptyList();
    }

    private void record(Class<?> entityClass, Object primaryKey, CacheOutcome outcome) {
        List<String> accesses = currentAccesses.get();
        if (accesses != null && entityClass != null) {
            String target = primaryKey != null
                    ? String.format("%s (id=%s)", entityClass.getSimpleName(), primaryKey)
                    : entityClass.getSimpleName();
            accesses.add(String.format("[L2 Cache] %s: %s", target, outcome.getLabel()));
        }
    }

    @Getter
    public enum CacheOutcome {
        HIT("L2 hit"),
        MISS("L2 miss");

        private final String label;

        CacheOutcome(String label) {
            this.label = label;
        }
    }
}
