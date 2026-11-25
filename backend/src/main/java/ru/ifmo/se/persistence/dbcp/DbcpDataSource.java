package ru.ifmo.se.persistence.dbcp;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.DelegatingConnection;

import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@SuppressWarnings("PMD.EmptyCatchBlock")
public final class DbcpDataSource extends BasicDataSource {
    private static final String REGISTRY_NAME = "java:comp/TransactionSynchronizationRegistry";
    private static final String RESOURCE_KEY = "ru.ifmo.se.dbcp.Connection";
    private final transient TransactionSynchronizationRegistry registry;

    {
        try {
            registry = InitialContext.doLookup(REGISTRY_NAME);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("this-escape")
    public DbcpDataSource() {
        super();
        setUrl(System.getenv("DB_URL"));
        setUsername(System.getenv("DB_USER"));
        setPassword(System.getenv("DB_PASSWORD"));
        setDriverClassName("org.postgresql.Driver");
        setInitialSize(5);
        setMaxTotal(20);
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (registry.getTransactionStatus() != Status.STATUS_ACTIVE) {
            return super.getConnection();
        }
        Connection connection = (Connection) registry.getResource(RESOURCE_KEY);
        if (connection == null) {
            connection = super.getConnection();
            connection.setAutoCommit(false);
            registry.putResource(RESOURCE_KEY, connection);
            registry.registerInterposedSynchronization(new JtaSynchronization(connection));
        }
        return new ManagedConnection(connection);
    }

    private static class ManagedConnection extends DelegatingConnection<Connection> {
        ManagedConnection(Connection conn) {
            super(conn);
        }

        @Override
        public void close() {
        }
    }

    private record JtaSynchronization(Connection connection) implements Synchronization {
        @Override
        public void beforeCompletion() {
        }

        @Override
        public void afterCompletion(int status) {
            try (connection) {
                if (status == Status.STATUS_COMMITTED) {
                    connection.commit();
                } else {
                    connection.rollback();
                }
            } catch (SQLException ignore) {
            }
        }
    }
}
