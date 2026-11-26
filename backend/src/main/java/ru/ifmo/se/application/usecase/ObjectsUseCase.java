package ru.ifmo.se.application.usecase;

import ru.ifmo.se.application.dto.command.FileSnapshotUpload;
import ru.ifmo.se.application.dto.result.ObjectsResult;

public interface ObjectsUseCase {
    ObjectsResult create(FileSnapshotUpload snapshotUpload, String username);
}
