package ru.ifmo.se.presentation.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import ru.ifmo.se.application.dto.command.FileSnapshotUpload;
import ru.ifmo.se.application.dto.result.ObjectsResult;
import ru.ifmo.se.application.exception.ImportFileReadException;
import ru.ifmo.se.application.exception.MissingImportFileException;
import ru.ifmo.se.application.usecase.ObjectsUseCase;
import ru.ifmo.se.presentation.dto.response.BatchObjectsResponse;
import ru.ifmo.se.presentation.mapper.ObjectsPresentationMapper;

import java.io.IOException;
import java.io.InputStream;

@Path("/objects")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.MULTIPART_FORM_DATA)
@RolesAllowed("USER")
public class ObjectsResource {

    @Inject
    private ObjectsUseCase objectsUseCase;

    @Inject
    private ObjectsPresentationMapper objectsMapper;

    @POST
    public Response importObjects(@FormParam("file") EntityPart filePart,
                                  @Context SecurityContext securityContext) {
        if (filePart == null) {
            throw new MissingImportFileException();
        }
        FileSnapshotUpload snapshotUpload = toSnapshotUpload(filePart);
        String username = securityContext.getUserPrincipal().getName();
        ObjectsResult result = objectsUseCase.create(snapshotUpload, username);
        BatchObjectsResponse response = objectsMapper.toResponse(result);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    private FileSnapshotUpload toSnapshotUpload(EntityPart filePart) {
        try (InputStream content = filePart.getContent()) {
            byte[] rawBytes = content != null ? content.readAllBytes() : new byte[0];
            String fileName = filePart.getFileName().orElse(null);
            String contentType = filePart.getMediaType() != null ? filePart.getMediaType().toString() : null;
            return new FileSnapshotUpload(rawBytes, fileName, contentType);
        } catch (IOException e) {
            throw new ImportFileReadException(e);
        }
    }
}
