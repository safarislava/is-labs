package ru.ifmo.se.persistence.s3;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ru.ifmo.se.application.dto.command.FileSnapshotUpload;
import ru.ifmo.se.application.dto.result.SnapshotDownloadResult;
import ru.ifmo.se.application.repository.SnapshotRepository;

@ApplicationScoped
public class S3SnapshotStorage implements SnapshotRepository {
    @Inject
    private S3DataSource s3DataSource;

    @Inject
    private S3ObjectClient objectClient;

    @Inject
    private S3PreparedStorage preparedStorage;

    @Override
    public String saveSnapshot(FileSnapshotUpload upload) {
        String safeFileName = S3SnapshotNaming.sanitizeFileName(upload.fileName());
        String contentType = S3SnapshotNaming.resolveContentType(upload.contentType());
        String finalKey = S3SnapshotNaming.buildFinalKey(safeFileName);
        S3XAResource xaResource = new S3XAResource(
                preparedStorage,
                new S3PreparedStorage.PreparedUpload(safeFileName, finalKey, upload.content(), contentType)
        );
        s3DataSource.enlist(xaResource);
        return finalKey;
    }

    @Override
    public SnapshotDownloadResult findSnapshot(String key) {
        S3ObjectClient.ObjectPayload payload = objectClient.getObject(key);
        return new SnapshotDownloadResult(
                payload.content(),
                S3SnapshotNaming.extractFileName(key),
                payload.contentType(),
                payload.content().length
        );
    }
}
