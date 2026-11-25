package ru.ifmo.se.persistence.dbcp;

import org.eclipse.persistence.sessions.DatabaseLogin;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.sessions.SessionCustomizer;

public class DbcpSessionCustomizer implements SessionCustomizer {

    @Override
    public void customize(Session session) {
        DatabaseLogin login = session.getLogin();
        login.setConnector(new DbcpConnector(new DbcpDataSource()));
    }
}
