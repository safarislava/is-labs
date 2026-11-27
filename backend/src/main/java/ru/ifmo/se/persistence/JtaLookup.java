package ru.ifmo.se.persistence;

import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;

import javax.naming.InitialContext;
import javax.naming.NamingException;

public final class JtaLookup {
    private JtaLookup() {
    }

    public static TransactionSynchronizationRegistry registry() {
        return lookup("java:comp/TransactionSynchronizationRegistry");
    }

    public static TransactionManager transactionManager() {
        return lookup("java:jboss/TransactionManager");
    }

    private static <T> T lookup(String name) {
        try {
            return InitialContext.doLookup(name);
        } catch (NamingException e) {
            throw new IllegalStateException("Failed JNDI lookup: " + name, e);
        }
    }
}
