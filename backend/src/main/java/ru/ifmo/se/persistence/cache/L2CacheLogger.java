package ru.ifmo.se.persistence.cache;

import lombok.Getter;
import org.eclipse.persistence.internal.identitymaps.CacheKey;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class L2CacheLogger {
    private final ThreadLocal<Map<String, Set<CacheOutcome>>> currentAccesses = new ThreadLocal<>();

    public void beginTracking() {
        currentAccesses.set(new LinkedHashMap<>());
    }

    public boolean isTracking() {
        return currentAccesses.get() != null;
    }

    public <T> T recordValue(Class<?> entityClass, T value) {
        record(entityClass, value != null ? CacheOutcome.HIT : CacheOutcome.MISS);
        return value;
    }

    public void recordKey(Class<?> entityClass, CacheKey cacheKey) {
        boolean hit = cacheKey != null
                && cacheKey.getInvalidationState() != CacheKey.CACHE_KEY_INVALID
                && cacheKey.getObject() != null;
        record(entityClass, hit ? CacheOutcome.HIT : CacheOutcome.MISS);
    }

    public Map<String, String> finishTracking() {
        Map<String, Set<CacheOutcome>> accesses = currentAccesses.get();
        currentAccesses.remove();
        if (accesses == null) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new HashMap<>(accesses.size());
        for (Map.Entry<String, Set<CacheOutcome>> entry : accesses.entrySet()) {
            String summary = entry.getValue().stream()
                    .map(CacheOutcome::getLabel)
                    .collect(Collectors.joining(", "));
            result.put(entry.getKey(), summary);
        }
        return result;
    }

    private void record(Class<?> entityClass, CacheOutcome outcome) {
        Map<String, Set<CacheOutcome>> accesses = currentAccesses.get();
        if (accesses != null && entityClass != null) {
            accesses.computeIfAbsent(entityClass.getSimpleName(), ignored -> EnumSet.noneOf(CacheOutcome.class))
                    .add(outcome);
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
