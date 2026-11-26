package ru.ifmo.se.application.tx;

public interface TwoPhaseParticipant {
    String getName();

    void prepare();

    void commit();

    void rollback();
}
