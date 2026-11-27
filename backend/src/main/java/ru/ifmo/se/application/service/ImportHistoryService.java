package ru.ifmo.se.application.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.ifmo.se.application.annotation.LogCache;
import ru.ifmo.se.application.dto.result.ImportOperationResult;
import ru.ifmo.se.application.dto.result.PageResult;
import ru.ifmo.se.application.dto.result.SnapshotDownloadResult;
import ru.ifmo.se.application.exception.ImportOperationAccessDeniedException;
import ru.ifmo.se.application.exception.ImportOperationNotFoundException;
import ru.ifmo.se.application.exception.SnapshotNotFoundException;
import ru.ifmo.se.application.mapper.ImportOperationMapper;
import ru.ifmo.se.application.repository.ImportOperationRepository;
import ru.ifmo.se.application.repository.SnapshotRepository;
import ru.ifmo.se.application.repository.UserRepository;
import ru.ifmo.se.application.usecase.ImportHistoryUseCase;
import ru.ifmo.se.persistence.entity.ImportOperation;
import ru.ifmo.se.persistence.entity.ImportStatus;
import ru.ifmo.se.persistence.entity.User;

import java.time.ZonedDateTime;
import java.util.List;

@ApplicationScoped
@LogCache
public class ImportHistoryService implements ImportHistoryUseCase {

    @Inject
    private ImportOperationRepository importOperationRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private ImportOperationMapper mapper;

    @Inject
    private SnapshotRepository snapshotRepository;

    @Override
    public List<ImportOperationResult> getHistory(String username, boolean isAdmin) {
        List<ImportOperation> operations = isAdmin
                ? importOperationRepository.findAll()
                : importOperationRepository.findAllByUsername(username);

        return operations.stream()
                .map(mapper::toResult)
                .toList();
    }

    @Override
    public PageResult<ImportOperationResult> getHistory(String username, boolean isAdmin, int page, int size) {
        PageResult<ImportOperation> pageResult = isAdmin
                ? importOperationRepository.findPage(page, size)
                : importOperationRepository.findPageByUsername(username, page, size);

        List<ImportOperationResult> results = pageResult.getContent().stream()
                .map(mapper::toResult)
                .toList();

        return new PageResult<>(results, pageResult.getTotal(), pageResult.getPage(), pageResult.getSize());
    }

    @Override
    public SnapshotDownloadResult downloadSnapshot(int operationId, String username, boolean isAdmin) {
        ImportOperation operation = importOperationRepository.findById(operationId)
                .orElseThrow(() -> new ImportOperationNotFoundException(operationId));

        if (!isAdmin && (operation.getUser() == null || !operation.getUser().getUsername().equals(username))) {
            throw new ImportOperationAccessDeniedException(operationId);
        }

        String s3Key = operation.getS3Key();
        if (s3Key == null || s3Key.isBlank()) {
            throw new SnapshotNotFoundException(operationId);
        }

        return snapshotRepository.findSnapshot(s3Key);
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public ImportOperationResult createSuccessOperation(String username, int addedObjectsCount, String s3Key) {
        ImportOperation operation = buildOperation(username, ImportStatus.SUCCESS);
        operation.setAddedObjectsCount(addedObjectsCount);
        operation.setS3Key(s3Key);
        return mapper.toResult(importOperationRepository.save(operation));
    }

    @Transactional
    public ImportOperationResult createFailedOperation(String username, ImportStatus status) {
        ImportOperation operation = buildOperation(username, status);
        return mapper.toResult(importOperationRepository.save(operation));
    }

    private ImportOperation buildOperation(String username, ImportStatus status) {
        User user = userRepository.findByUsername(username).orElseThrow();
        ImportOperation operation = new ImportOperation();
        operation.setUser(user);
        operation.setStatus(status);
        operation.setCreationDate(ZonedDateTime.now());
        return operation;
    }
}
