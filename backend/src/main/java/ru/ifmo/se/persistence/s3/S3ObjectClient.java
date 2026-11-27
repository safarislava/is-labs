package ru.ifmo.se.persistence.s3;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ru.ifmo.se.application.exception.SnapshotCommitException;
import ru.ifmo.se.application.exception.SnapshotDownloadException;
import ru.ifmo.se.application.exception.SnapshotEtagMismatchException;
import ru.ifmo.se.application.exception.SnapshotListException;
import ru.ifmo.se.application.exception.SnapshotNotFoundException;
import ru.ifmo.se.application.exception.SnapshotSizeMismatchException;
import ru.ifmo.se.application.exception.SnapshotUploadException;
import ru.ifmo.se.application.exception.SnapshotVerifyException;
import ru.ifmo.se.application.exception.StorageException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

@ApplicationScoped
@SuppressWarnings("PMD.EmptyCatchBlock")
class S3ObjectClient {
    @Inject
    private S3DataSource s3DataSource;

    ObjectPayload getObject(String key) {
        try {
            ResponseBytes<GetObjectResponse> response = s3DataSource.getS3Client()
                    .getObjectAsBytes(b -> b.bucket(s3DataSource.getBucket()).key(key));
            return new ObjectPayload(
                    response.asByteArray(),
                    S3SnapshotNaming.resolveContentType(response.response().contentType())
            );
        } catch (NoSuchKeyException e) {
            throw new SnapshotNotFoundException(key, e);
        } catch (SdkException | UncheckedIOException e) {
            throw new SnapshotDownloadException(e);
        }
    }

    void putAndVerify(String stagingKey, byte[] content, String contentType, Map<String, String> metadata) {
        String bucket = s3DataSource.getBucket();
        PutObjectResponse put = execute(
                () -> s3DataSource.getS3Client().putObject(
                        b -> b.bucket(bucket)
                                .key(stagingKey)
                                .contentType(contentType)
                                .contentLength((long) content.length)
                                .metadata(metadata),
                        RequestBody.fromBytes(content)),
                SnapshotUploadException::new
        );
        HeadObjectResponse head = execute(
                () -> s3DataSource.getS3Client().headObject(b -> b.bucket(bucket).key(stagingKey)),
                SnapshotVerifyException::new
        );
        if (!Objects.equals(head.contentLength(), (long) content.length)) {
            throw new SnapshotSizeMismatchException();
        }
        if (put.eTag() != null && !Objects.equals(put.eTag(), head.eTag())) {
            throw new SnapshotEtagMismatchException();
        }
    }

    ObjectMetadata getObjectMetadata(String key) {
        String bucket = s3DataSource.getBucket();
        return execute(() -> {
            HeadObjectResponse head = s3DataSource.getS3Client().headObject(b -> b.bucket(bucket).key(key));
            return new ObjectMetadata(head.contentType(), head.metadata());
        }, SnapshotCommitException::new);
    }

    void moveObject(String sourceKey, String targetKey, String contentType) {
        String bucket = s3DataSource.getBucket();
        execute(() -> {
            s3DataSource.getS3Client().copyObject(b -> b.sourceBucket(bucket)
                    .sourceKey(sourceKey)
                    .destinationBucket(bucket)
                    .destinationKey(targetKey)
                    .contentType(contentType)
                    .metadata(Map.of())
                    .metadataDirective(MetadataDirective.REPLACE));
            s3DataSource.getS3Client().deleteObject(b -> b.bucket(bucket).key(sourceKey));
            return null;
        }, SnapshotCommitException::new);
    }

    void delete(String key) {
        try {
            s3DataSource.getS3Client().deleteObject(b -> b.bucket(s3DataSource.getBucket()).key(key));
        } catch (NoSuchKeyException ignore) {
        } catch (SdkException | UncheckedIOException e) {
            throw new SnapshotCommitException(e);
        }
    }

    void deleteQuietly(String key) {
        try {
            delete(key);
        } catch (SnapshotCommitException ignore) {
        }
    }

    void deleteByPrefix(String prefix) {
        for (String key : listKeys(prefix)) {
            delete(key);
        }
    }

    String findFirstKey(String prefix) {
        List<String> keys = listKeys(prefix);
        return keys.isEmpty() ? null : keys.get(0);
    }

    List<String> listKeys(String prefix) {
        return execute(
                () -> s3DataSource.getS3Client()
                        .listObjectsV2Paginator(b -> b.bucket(s3DataSource.getBucket()).prefix(prefix))
                        .contents()
                        .stream()
                        .map(S3Object::key)
                        .toList(),
                SnapshotListException::new
        );
    }

    private static <T> T execute(Supplier<T> action, Function<Throwable, StorageException> errorMapper) {
        try {
            return action.get();
        } catch (SdkException | UncheckedIOException e) {
            throw errorMapper.apply(e);
        }
    }

    record ObjectPayload(byte[] content, String contentType) {
    }

    record ObjectMetadata(String contentType, Map<String, String> userMetadata) {
    }
}
