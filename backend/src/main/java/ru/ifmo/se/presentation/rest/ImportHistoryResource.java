package ru.ifmo.se.presentation.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import ru.ifmo.se.application.dto.result.ImportOperationResult;
import ru.ifmo.se.application.dto.result.PageResult;
import ru.ifmo.se.application.dto.result.SnapshotDownloadResult;
import ru.ifmo.se.application.usecase.ImportHistoryUseCase;
import ru.ifmo.se.presentation.dto.response.ImportOperationPageResponse;
import ru.ifmo.se.presentation.mapper.ImportPresentationMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Path("/objects/history")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("USER")
public class ImportHistoryResource {
    @Inject
    private ImportHistoryUseCase importHistoryUseCase;

    @Inject
    private ImportPresentationMapper importMapper;

    @GET
    public Response getHistory(
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @QueryParam("size") @DefaultValue("10") @Min(1) @Max(100) int size,
            @Context SecurityContext securityContext) {
        String username = securityContext.getUserPrincipal().getName();
        boolean isAdmin = securityContext.isUserInRole("ADMIN");

        PageResult<ImportOperationResult> results = importHistoryUseCase.getHistory(username, isAdmin, page, size);
        ImportOperationPageResponse response = importMapper.toPageResponse(results);

        return Response.ok(response).build();
    }

    @GET
    @Path("/{id}/file")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadSnapshot(
            @PathParam("id") @Min(1) int id,
            @Context SecurityContext securityContext) {
        String username = securityContext.getUserPrincipal().getName();
        boolean isAdmin = securityContext.isUserInRole("ADMIN");

        SnapshotDownloadResult snapshot = importHistoryUseCase.downloadSnapshot(id, username, isAdmin);
        String encodedName = URLEncoder.encode(snapshot.fileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        String asciiFallback = snapshot.fileName().replaceAll("[^A-Za-z0-9._-]", "_");

        return Response.ok(snapshot.content(), snapshot.contentType())
                .header("Content-Disposition",
                        "attachment; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + encodedName)
                .header("Content-Length", snapshot.fileSize())
                .build();
    }
}
