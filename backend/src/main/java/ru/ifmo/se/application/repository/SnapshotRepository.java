package ru.ifmo.se.application.repository;

import ru.ifmo.se.application.dto.command.FileSnapshotUpload;
import ru.ifmo.se.application.dto.result.SnapshotDownloadResult;

public interface SnapshotRepository {
    String saveSnapshot(int operationId, FileSnapshotUpload upload);

    SnapshotDownloadResult findSnapshot(String key);
}
