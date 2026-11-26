package ru.ifmo.se.application.dto.event;

import lombok.Getter;
import lombok.Setter;
import ru.ifmo.se.persistence.entity.ImportStatus;

import java.time.ZonedDateTime;

@Getter
@Setter
public class ImportEvent {
    public enum Phase {
        START,
        FINISH
    }

    private int operationId;
    private String username;
    private ImportStatus status;
    private Integer addedObjectsCount;
    private Phase phase;
    private ZonedDateTime creationDate;

    public static ImportEvent start(String username) {
        ImportEvent event = new ImportEvent();
        event.username = username;
        event.status = ImportStatus.IN_PROGRESS;
        event.phase = Phase.START;
        return event;
    }

    public static ImportEvent finishSuccess(int operationId,
                                            String username,
                                            int addedObjectsCount,
                                            ZonedDateTime creationDate) {
        ImportEvent event = new ImportEvent();
        event.operationId = operationId;
        event.username = username;
        event.status = ImportStatus.SUCCESS;
        event.addedObjectsCount = addedObjectsCount;
        event.creationDate = creationDate;
        event.phase = Phase.FINISH;
        return event;
    }

    public static ImportEvent finishFailure(int operationId,
                                            String username,
                                            ImportStatus failureStatus,
                                            ZonedDateTime creationDate) {
        ImportEvent event = new ImportEvent();
        event.operationId = operationId;
        event.username = username;
        event.status = failureStatus != null ? failureStatus : ImportStatus.FAILED;
        event.creationDate = creationDate;
        event.phase = Phase.FINISH;
        return event;
    }
}
