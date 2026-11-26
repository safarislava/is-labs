package ru.ifmo.se.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import ru.ifmo.se.application.dto.command.FileSnapshotUpload;
import ru.ifmo.se.application.dto.command.ObjectsCreateCommand;
import ru.ifmo.se.application.exception.EmptyImportFileException;
import ru.ifmo.se.application.exception.EmptyImportJsonException;
import ru.ifmo.se.application.exception.InvalidImportJsonException;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

@ApplicationScoped
public class BatchImportParser {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    private Validator validator;

    public ObjectsCreateCommand parse(FileSnapshotUpload snapshotUpload) {
        if (snapshotUpload == null || snapshotUpload.content() == null || snapshotUpload.content().length == 0) {
            throw new EmptyImportFileException();
        }
        ObjectsCreateCommand command = deserialize(snapshotUpload.content());
        normalizeCollections(command);
        validate(command);
        return command;
    }

    private ObjectsCreateCommand deserialize(byte[] rawBytes) {
        try {
            ObjectsCreateCommand command = objectMapper.readValue(rawBytes, ObjectsCreateCommand.class);
            if (command == null) {
                throw new EmptyImportJsonException();
            }
            return command;
        } catch (IOException e) {
            throw new InvalidImportJsonException(e);
        }
    }

    private static void normalizeCollections(ObjectsCreateCommand command) {
        if (command.getCreatures() == null) {
            command.setCreatures(Collections.emptyList());
        }
        if (command.getRings() == null) {
            command.setRings(Collections.emptyList());
        }
        if (command.getCities() == null) {
            command.setCities(Collections.emptyList());
        }
    }

    private void validate(ObjectsCreateCommand command) {
        Set<ConstraintViolation<ObjectsCreateCommand>> violations = validator.validate(command);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
