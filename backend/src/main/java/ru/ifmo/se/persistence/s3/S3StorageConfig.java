package ru.ifmo.se.persistence.s3;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@ApplicationScoped
public class S3StorageConfig {
    @Getter
    private String bucket;

    @Getter
    private long stagingTtlSeconds;

    @Getter
    private int txTimeoutSeconds;

    @Getter
    private S3Client s3Client;

    @PostConstruct
    public void init() {
        String endpoint = System.getenv("S3_ENDPOINT");
        String region = System.getenv("S3_REGION");
        this.bucket = System.getenv("S3_BUCKET");
        String accessKey = System.getenv("S3_ACCESS_KEY");
        String secretKey = System.getenv("S3_SECRET_KEY");
        this.stagingTtlSeconds = Long.parseLong(System.getenv("S3_STAGING_TTL_SECONDS"));
        this.txTimeoutSeconds = Integer.parseInt(System.getenv("IMPORT_TX_TIMEOUT_SECONDS"));

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
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
}
