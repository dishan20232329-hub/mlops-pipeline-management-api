package com.mlops.resource;

import com.mlops.db.InMemoryDatabase;
import com.mlops.model.MLWorkspace;
import com.mlops.exception.WorkspaceNotEmptyException;

import javax.ws.rs.*;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;

@Path("/workspaces")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkspaceResource {

    @GET
    public Response getAllWorkspaces() {
        CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge(60); // Cache for 60 seconds
        cacheControl.setPrivate(false); // Make it public so intermediate proxies can cache it
        
        Collection<MLWorkspace> workspaces = InMemoryDatabase.getWorkspaces().values();
        return Response.ok(workspaces).cacheControl(cacheControl).build();
    }

    @POST
    public Response createWorkspace(MLWorkspace workspace) {
        if (workspace == null || workspace.getId() == null || workspace.getId().trim().isEmpty()) {
            throw new BadRequestException("Workspace ID must be provided.");
        }

        String id = workspace.getId().trim();
        workspace.setId(id);

        if (InMemoryDatabase.getWorkspaces().containsKey(id)) {
            throw new ClientErrorException("Workspace with ID '" + id + "' already exists.", Response.Status.CONFLICT);
        }

        if (workspace.getModelIds() == null) {
            workspace.setModelIds(new ArrayList<>());
        }

        InMemoryDatabase.getWorkspaces().put(id, workspace);
        return Response.status(Response.Status.CREATED).entity(workspace).build();
    }

    @GET
    @Path("/{workspaceId}")
    public MLWorkspace getWorkspaceById(@PathParam("workspaceId") String workspaceId) {
        MLWorkspace workspace = InMemoryDatabase.getWorkspaces().get(workspaceId);
        if (workspace == null) {
            throw new NotFoundException("Workspace with ID '" + workspaceId + "' not found.");
        }
        return workspace;
    }

    @DELETE
    @Path("/{workspaceId}")
    public Response deleteWorkspace(@PathParam("workspaceId") String workspaceId) {
        MLWorkspace workspace = InMemoryDatabase.getWorkspaces().get(workspaceId);
        if (workspace == null) {
            throw new NotFoundException("Workspace with ID '" + workspaceId + "' not found.");
        }

        // Business Logic Constraint: A workspace cannot be deleted if it still has models assigned to it.
        if (workspace.getModelIds() != null && !workspace.getModelIds().isEmpty()) {
            throw new WorkspaceNotEmptyException("Workspace '" + workspaceId + "' cannot be deleted because it still has "
                    + workspace.getModelIds().size() + " model(s) assigned to it.");
        }

        // Double check the models database for safety
        boolean hasLinkedModels = InMemoryDatabase.getModels().values().stream()
                .anyMatch(model -> workspaceId.equals(model.getWorkspaceId()));
        if (hasLinkedModels) {
            throw new WorkspaceNotEmptyException("Workspace '" + workspaceId + "' cannot be deleted due to existing model references.");
        }

        InMemoryDatabase.getWorkspaces().remove(workspaceId);
        return Response.noContent().build();
    }
}
