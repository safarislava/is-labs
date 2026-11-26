package ru.ifmo.se.application.tx;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class TwoPhaseCommitCoordinator {
    private static final Logger LOGGER = Logger.getLogger(TwoPhaseCommitCoordinator.class.getName());

    public void execute(List<TwoPhaseParticipant> prepareOrder, List<TwoPhaseParticipant> commitOrder) {
        prepareAll(prepareOrder);
        commitAll(commitOrder);
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private void prepareAll(List<TwoPhaseParticipant> participants) {
        int preparedCount = 0;
        for (TwoPhaseParticipant participant : participants) {
            try {
                LOGGER.info(() -> logMessage(participant, "prepare"));
                participant.prepare();
                preparedCount++;
                LOGGER.info(() -> logMessage(participant, "prepare ok"));
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, logMessage(participant, "prepare failed"), e);
                rollbackSubset(participants, preparedCount + 1);
                throw e;
            }
        }
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private void commitAll(List<TwoPhaseParticipant> participants) {
        for (TwoPhaseParticipant participant : participants) {
            try {
                LOGGER.info(() -> logMessage(participant, "commit"));
                participant.commit();
                LOGGER.info(() -> logMessage(participant, "commit ok"));
            } catch (RuntimeException e) {
                LOGGER.log(Level.SEVERE, logMessage(participant, "commit failed"), e);
                rollbackSubset(participants, participants.size());
                throw e;
            }
        }
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private void rollbackSubset(List<TwoPhaseParticipant> participants, int count) {
        for (int i = Math.min(count, participants.size()) - 1; i >= 0; i--) {
            TwoPhaseParticipant participant = participants.get(i);
            try {
                LOGGER.info(() -> logMessage(participant, "rollback"));
                participant.rollback();
                LOGGER.info(() -> logMessage(participant, "rollback ok"));
            } catch (RuntimeException rollbackEx) {
                LOGGER.log(Level.WARNING, logMessage(participant, "rollback failed"), rollbackEx);
            }
        }
    }

    private static String logMessage(TwoPhaseParticipant participant, String action) {
        return participant.getName() + " " + action;
    }
}
