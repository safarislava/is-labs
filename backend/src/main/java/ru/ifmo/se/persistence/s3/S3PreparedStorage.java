package ru.ifmo.se.persistence.s3;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ru.ifmo.se.application.exception.StorageException;

import java.util.Map;
import java.util.Optional;
import javax.transaction.xa.Xid;

@ApplicationScoped
class S3PreparedStorage {
    private static final String PREPARED_PREFIX = "prepared/";
    private static final String META_FINAL_KEY = "final-key";

    @Inject
    private S3ObjectClient objectClient;

    String stage(Xid xid, PreparedUpload upload) {
        String preparedKey = buildObjectKey(xid, upload.fileName());
        try {
            objectClient.putAndVerify(
                    preparedKey,
                    upload.content(),
                    upload.contentType(),
                    Map.of(META_FINAL_KEY, upload.finalKey())
            );
            return preparedKey;
        } catch (StorageException e) {
            objectClient.deleteQuietly(preparedKey);
            throw e;
        }
    }

    void promote(Xid xid, String knownPreparedKey, PreparedUpload upload) {
        String preparedKey = knownPreparedKey != null
                ? knownPreparedKey
                : objectClient.findFirstKey(buildTransactionPrefix(xid));
        if (preparedKey == null) {
            return;
        }

        String targetKey;
        String targetContentType;
        if (upload != null) {
            targetKey = upload.finalKey();
            targetContentType = upload.contentType();
        } else {
            S3ObjectClient.ObjectMetadata metadata = objectClient.getObjectMetadata(preparedKey);
            targetKey = metadata.userMetadata().get(META_FINAL_KEY);
            targetContentType = S3SnapshotNaming.resolveContentType(metadata.contentType());
        }
        objectClient.moveObject(preparedKey, targetKey, targetContentType);
    }

    void abort(Xid xid, String knownPreparedKey) {
        if (knownPreparedKey != null) {
            objectClient.delete(knownPreparedKey);
        } else {
            objectClient.deleteByPrefix(buildTransactionPrefix(xid));
        }
    }

    Xid[] listPreparedXids() {
        return objectClient.listKeys(PREPARED_PREFIX).stream()
                .map(S3PreparedStorage::extractEncodedXid)
                .flatMap(Optional::stream)
                .distinct()
                .map(XidCodec::decode)
                .flatMap(Optional::stream)
                .toArray(Xid[]::new);
    }

    private static String buildTransactionPrefix(Xid xid) {
        return PREPARED_PREFIX + XidCodec.encode(xid) + "/";
    }

    private static String buildObjectKey(Xid xid, String fileName) {
        return buildTransactionPrefix(xid) + fileName;
    }

    private static Optional<String> extractEncodedXid(String objectKey) {
        if (objectKey == null || !objectKey.startsWith(PREPARED_PREFIX)) {
            return Optional.empty();
        }
        int end = objectKey.indexOf('/', PREPARED_PREFIX.length());
        return end > PREPARED_PREFIX.length()
                ? Optional.of(objectKey.substring(PREPARED_PREFIX.length(), end))
                : Optional.empty();
    }

    record PreparedUpload(String fileName, String finalKey, byte[] content, String contentType) {
        PreparedUpload {
            content = content.clone();
        }
    }
}
