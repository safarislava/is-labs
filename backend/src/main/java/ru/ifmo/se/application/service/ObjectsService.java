package ru.ifmo.se.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.ifmo.se.application.annotation.LogCache;
import ru.ifmo.se.application.dto.command.FileSnapshotUpload;
import ru.ifmo.se.application.dto.command.ObjectsCreateCommand;
import ru.ifmo.se.application.dto.event.ImportNotificationEvent;
import ru.ifmo.se.application.dto.result.ImportOperationResult;
import ru.ifmo.se.application.dto.result.ObjectsResult;
import ru.ifmo.se.application.repository.SnapshotRepository;
import ru.ifmo.se.application.usecase.ObjectsUseCase;

import java.util.Map;

@ApplicationScoped
@LogCache
public class ObjectsService implements ObjectsUseCase {

    @Inject
    private ObjectsService self;

    @Inject
    private Event<ImportNotificationEvent> notificationPublisher;

    @Inject
    private BatchExecutionService batchExecutionService;

    @Inject
    private BatchImportParser batchImportParser;

    @Inject
    private SnapshotRepository snapshotRepository;

    @Inject
    private ImportHistoryService importHistoryService;

    @Inject
    private ImportFailureClassifier failureClassifier;

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public ObjectsResult create(FileSnapshotUpload snapshotUpload, String username) {
        ObjectsResult result = new ObjectsResult();
        try {
            ImportOperationResult completed = self.execute(snapshotUpload, username, result);
            notificationPublisher.fire(new ImportNotificationEvent(username, completed));
            return result;
        } catch (RuntimeException e) {
            ImportOperationResult failed = importHistoryService.createFailedOperation(
                    username,
                    failureClassifier.classify(e)
            );
            notificationPublisher.fire(new ImportNotificationEvent(username, failed));
            throw e;
        }
    }

    @Transactional
    public ImportOperationResult execute(FileSnapshotUpload snapshotUpload,
                                         String username,
                                         ObjectsResult result) {
        ObjectsCreateCommand command = batchImportParser.parse(snapshotUpload);

        String snapshotKey = snapshotRepository.saveSnapshot(snapshotUpload);

        Map<Integer, Integer> ringRefs = batchExecutionService.createRings(command.getRings(), result.getRings());
        Map<Integer, Integer> cityRefs = batchExecutionService.createCities(command.getCities(), result.getCities());
        batchExecutionService.createCreatures(command.getCreatures(), ringRefs, cityRefs, result.getCreatures());

        int count = result.getRings().size() + result.getCities().size() + result.getCreatures().size();
        return importHistoryService.createSuccessOperation(username, count, snapshotKey);
    }
}
