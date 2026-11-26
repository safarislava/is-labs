package ru.ifmo.se.persistence;

import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.sessions.SessionCustomizer;
import ru.ifmo.se.persistence.cache.InfinispanCacheSessionCustomizer;
import ru.ifmo.se.persistence.dbcp.DbcpSessionCustomizer;

import java.util.List;

public class AppSessionCustomizer implements SessionCustomizer {

    private final List<SessionCustomizer> customizers = List.of(
            new DbcpSessionCustomizer(),
            new InfinispanCacheSessionCustomizer()
    );

    @Override
    public void customize(Session session) throws Exception {
        for (SessionCustomizer customizer : customizers) {
            customizer.customize(session);
        }
    }
}
