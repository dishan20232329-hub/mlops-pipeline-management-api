package com.mlops.resource;

import com.mlops.db.InMemoryDatabase;
import com.mlops.model.MachineLearningModel;
import com.mlops.model.MLWorkspace;
import com.mlops.exception.LinkedWorkspaceNotFoundException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/models")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ModelResource {

    @GET
    public Collection<MachineLearningModel> getModels(@QueryParam("status") String status) {
        Collection<MachineLearningModel> allModels = InMemoryDatabase.getModels().values();
        if (status == null || status.trim().isEmpty()) {
            return allModels;
        }

        String statusFilter = status.trim().toUpperCase();
        return allModels.stream()
                .filter(m -> m.getStatus() != null && m.getStatus().equalsIgnoreCase(statusFilter))
                .collect(Collectors.toList());
    }

    @POST
    public Response registerModel(MachineLearningModel model) {
        if (model == null) {
            throw new BadRequestException("Model data is required.");
        }

        String wsId = model.getWorkspaceId();
        if (wsId == null || wsId.trim().isEmpty()) {
            throw new LinkedWorkspaceNotFoundException("Workspace ID is required to link the model.");
        }

        wsId = wsId.trim();
        model.setWorkspaceId(wsId);

        // Dependency Validation: Ensure the linked workspace actually exists in the database
        MLWorkspace workspace = InMemoryDatabase.getWorkspaces().get(wsId);
        if (workspace == null) {
            throw new LinkedWorkspaceNotFoundException("Cannot register model. Workspace with ID '" + wsId + "' does not exist.");
        }

        // Server-Side ID Generation: Security & data integrity best practice
        String generatedId = "MOD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        model.setId(generatedId);

        // If status is not provided, default to "TRAINING"
        if (model.getStatus() == null || model.getStatus().trim().isEmpty()) {
            model.setStatus("TRAINING");
        } else {
            model.setStatus(model.getStatus().trim().toUpperCase());
        }

        // Save to in-memory database
        InMemoryDatabase.getModels().put(generatedId, model);

        // Update the workspace's model ID list
        if (workspace.getModelIds() == null) {
            workspace.setModelIds(new ArrayList<>());
        }
        workspace.getModelIds().add(generatedId);

        return Response.status(Response.Status.CREATED).entity(model).build();
    }

    // Sub-Resource Locator Pattern: Delegates requests under /models/{modelId}/metrics to EvaluationMetricResource
    @Path("/{modelId}/metrics")
    public EvaluationMetricResource getEvaluationMetricResource(@PathParam("modelId") String modelId) {
        MachineLearningModel model = InMemoryDatabase.getModels().get(modelId);
        if (model == null) {
            throw new NotFoundException("Model with ID '" + modelId + "' not found.");
        }
        return new EvaluationMetricResource(modelId);
    }
}
