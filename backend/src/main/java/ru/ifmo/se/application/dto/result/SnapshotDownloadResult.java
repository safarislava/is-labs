package ru.ifmo.se.application.dto.result;

public record SnapshotDownloadResult(byte[] content, String fileName, String contentType, long fileSize) {
}
