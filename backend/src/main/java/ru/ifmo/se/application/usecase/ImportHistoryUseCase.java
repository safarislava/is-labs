package ru.ifmo.se.application.usecase;

import ru.ifmo.se.application.dto.result.ImportOperationResult;
import ru.ifmo.se.application.dto.result.PageResult;
import ru.ifmo.se.application.dto.result.SnapshotDownloadResult;

import java.util.List;

public interface ImportHistoryUseCase {
    List<ImportOperationResult> getHistory(String username, boolean isAdmin);

    PageResult<ImportOperationResult> getHistory(String username, boolean isAdmin, int page, int size);

    SnapshotDownloadResult downloadSnapshot(int operationId, String username, boolean isAdmin);
}
