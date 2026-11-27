package ru.ifmo.se.persistence.s3;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import lombok.Getter;
import ru.ifmo.se.application.exception.SnapshotUploadException;
import ru.ifmo.se.persistence.JtaLookup;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import javax.transaction.xa.XAResource;

@ApplicationScoped
public class S3DataSource {
    private TransactionManager transactionManager;

    @Getter
    private String bucket;

    @Getter
    private S3Client s3Client;

    @PostConstruct
    public void init() {
        this.transactionManager = JtaLookup.transactionManager();
        this.bucket = System.getenv("S3_BUCKET");

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(System.getenv("S3_ENDPOINT")))
                .region(Region.of(System.getenv("S3_REGION")))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(System.getenv("S3_ACCESS_KEY"), System.getenv("S3_SECRET_KEY"))))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .chunkedEncodingEnabled(false)
                        .build())
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
    }

    @PreDestroy
    public void destroy() {
        if (s3Client != null) {
            s3Client.close();
        }
    }

    public void enlist(XAResource xaResource) {
        try {
            Transaction tx = transactionManager.getTransaction();
            if (tx == null || !tx.enlistResource(xaResource)) {
                throw new SnapshotUploadException(
                        new IllegalStateException("Failed to enlist S3XAResource into active JTA transaction"));
            }
        } catch (RollbackException | SystemException e) {
            throw new SnapshotUploadException(e);
        }
    }
}
