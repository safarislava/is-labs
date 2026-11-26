package ru.ifmo.se.application.dto.command;

public record FileSnapshotUpload(byte[] content, String fileName, String contentType) {
}
