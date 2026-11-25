package ru.ifmo.se.persistence.dbcp;

import org.eclipse.persistence.exceptions.DatabaseException;
import org.eclipse.persistence.sessions.Connector;
import org.eclipse.persistence.sessions.Session;

import java.io.PrintWriter;
import java.io.Serial;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DbcpConnector implements Connector {
    @Serial
    private static final long serialVersionUID = 1L;

    private final transient DbcpDataSource dataSource;

    public DbcpConnector(DbcpDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection connect(Properties properties, Session session) {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw DatabaseException.sqlException(e, true);
        }
    }

    @Override
    public DbcpConnector clone() {
        try {
            return (DbcpConnector) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("Clone not supported", e);
        }
    }

    @Override
    public String getConnectionDetails() {
        return "Apache Commons DBCP2: " + dataSource.getUrl();
    }

    @Override
    public void toString(PrintWriter writer) {
        writer.println(getConnectionDetails());
    }
}
