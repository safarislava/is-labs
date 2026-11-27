package ru.ifmo.se.persistence.dbcp;

import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DelegatingConnection;
import org.postgresql.xa.PGXADataSource;
import ru.ifmo.se.persistence.JtaLookup;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;

@SuppressWarnings({"PMD.EmptyCatchBlock", "PMD.AvoidCatchingGenericException"})
public final class DbcpDataSource extends BasicDataSource {
    private static final String RESOURCE_KEY = "ru.ifmo.se.dbcp.Connection";
    private final transient TransactionSynchronizationRegistry registry;
    private final transient TransactionManager transactionManager;

    @SuppressWarnings("this-escape")
    public DbcpDataSource() {
        super();
        this.registry = JtaLookup.registry();
        this.transactionManager = JtaLookup.transactionManager();
        setUrl(System.getenv("DB_URL"));
        setInitialSize(5);
        setMaxTotal(20);
        setTestOnBorrow(true);
        setTestWhileIdle(true);
        setRollbackOnReturn(false);
    }

    @Override
    protected ConnectionFactory createConnectionFactory() {
        PGXADataSource xaDataSource = new PGXADataSource();
        if (getUrl() != null) {
            xaDataSource.setUrl(getUrl());
        }
        xaDataSource.setUser(System.getenv("DB_USER"));
        xaDataSource.setPassword(System.getenv("DB_PASSWORD"));
        return () -> new XaConnectionWrapper(xaDataSource.getXAConnection());
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (registry.getTransactionStatus() != Status.STATUS_ACTIVE) {
            return super.getConnection();
        }
        Connection connection = (Connection) registry.getResource(RESOURCE_KEY);
        if (connection == null) {
            connection = super.getConnection();
            enlist(connection);
            registry.putResource(RESOURCE_KEY, connection);
            registry.registerInterposedSynchronization(new JtaSynchronization(connection));
        }
        return new ManagedConnection(connection);
    }

    private void enlist(Connection connection) throws SQLException {
        boolean enlisted;
        try {
            Transaction tx = transactionManager.getTransaction();
            enlisted = tx != null && tx.enlistResource(connection.unwrap(XAResource.class));
        } catch (RollbackException | SystemException | SQLException | RuntimeException e) {
            closeQuietly(connection);
            throw new SQLException("Failed to enlist PostgreSQL XAResource in JTA transaction", e);
        }
        if (!enlisted) {
            closeQuietly(connection);
            throw new SQLException("Failed to enlist PostgreSQL XAResource in JTA transaction");
        }
    }

    private static void closeQuietly(Connection connection) {
        try {
            connection.close();
        } catch (SQLException ignore) {
        }
    }

    private static final class XaConnectionWrapper extends DelegatingConnection<Connection> {
        private final XAConnection xaConnection;

        private XaConnectionWrapper(XAConnection xaConnection) throws SQLException {
            super(xaConnection.getConnection());
            this.xaConnection = xaConnection;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return iface == XAResource.class || super.isWrapperFor(iface);
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            if (iface == XAResource.class) {
                return iface.cast(xaConnection.getXAResource());
            }
            return super.unwrap(iface);
        }

        @Override
        public void close() throws SQLException {
            try {
                super.close();
            } finally {
                xaConnection.close();
            }
        }
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
            closeQuietly(connection);
        }
    }
}
