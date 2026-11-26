package ru.ifmo.se.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import ru.ifmo.se.application.dto.event.ImportEvent;
import ru.ifmo.se.application.dto.event.ImportNotificationEvent;
import ru.ifmo.se.application.dto.result.ImportOperationResult;
import ru.ifmo.se.application.mapper.ImportOperationMapper;
import ru.ifmo.se.persistence.entity.ImportOperation;
import ru.ifmo.se.persistence.entity.ImportStatus;

@ApplicationScoped
public class ImportHistoryEventListener {

    @Inject
    private ImportHistoryService importHistoryService;

    @Inject
    private Event<ImportNotificationEvent> notificationPublisher;

    @Inject
    private ImportOperationMapper mapper;

    public void onImportEvent(@Observes ImportEvent event) {
        if (event.getPhase() == ImportEvent.Phase.START) {
            ImportOperation saved = importHistoryService.createInProgressOperation(event.getUsername());
            event.setOperationId(saved.getId());
            event.setCreationDate(saved.getCreationDate());
            notificationPublisher.fire(new ImportNotificationEvent(event.getUsername(), mapper.toResult(saved)));
            return;
        }

        ImportOperationResult result;
        if (event.getStatus() == ImportStatus.SUCCESS) {
            result = new ImportOperationResult(
                    event.getOperationId(),
                    ImportStatus.SUCCESS,
                    event.getUsername(),
                    event.getAddedObjectsCount(),
                    event.getCreationDate()
            );
        } else {
            ImportStatus failureStatus = event.getStatus() != null ? event.getStatus() : ImportStatus.FAILED;
            ImportOperation saved = importHistoryService.failOperation(event.getOperationId(), failureStatus);
            result = mapper.toResult(saved);
        }
        notificationPublisher.fire(new ImportNotificationEvent(event.getUsername(), result));
    }
}
