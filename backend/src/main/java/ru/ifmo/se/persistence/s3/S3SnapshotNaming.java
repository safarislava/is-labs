package ru.ifmo.se.persistence.s3;

import java.util.UUID;

final class S3SnapshotNaming {
    private static final String DEFAULT_CONTENT_TYPE = "application/json";
    private static final String DEFAULT_FILE_NAME = "import.json";

    private S3SnapshotNaming() {
    }

    static String resolveContentType(String rawType) {
        return rawType != null && !rawType.isBlank() ? rawType : DEFAULT_CONTENT_TYPE;
    }

    static String sanitizeFileName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return DEFAULT_FILE_NAME;
        }
        String baseName = rawName.replace('\\', '/');
        int lastSlash = baseName.lastIndexOf('/');
        if (lastSlash >= 0) {
            baseName = baseName.substring(lastSlash + 1);
        }
        String sanitized = baseName.replaceAll("[^A-Za-z0-9._-]", "_");
        return sanitized.isBlank() ? DEFAULT_FILE_NAME : sanitized;
    }

    static String buildFinalKey(String safeFileName) {
        return String.format("snapshots/%s_%s", UUID.randomUUID(), safeFileName);
    }

    static String extractFileName(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return DEFAULT_FILE_NAME;
        }
        int lastSlash = s3Key.lastIndexOf('/');
        String segment = lastSlash >= 0 ? s3Key.substring(lastSlash + 1) : s3Key;
        int underscore = segment.indexOf('_');
        return underscore >= 0 && underscore + 1 < segment.length()
                ? segment.substring(underscore + 1)
                : segment;
    }
}
