package com.mlops.resource;

import com.mlops.db.InMemoryDatabase;
import com.mlops.model.EvaluationMetric;
import com.mlops.model.MachineLearningModel;
import com.mlops.exception.ModelDeprecatedException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EvaluationMetricResource {
    private final String modelId;

    public EvaluationMetricResource(String modelId) {
        this.modelId = modelId;
    }

    @GET
    public Collection<EvaluationMetric> getMetrics() {
        // Return metrics associated with this specific model context
        return InMemoryDatabase.getOrCreateMetricsForModel(modelId);
    }

    @POST
    public Response addMetric(EvaluationMetric metric) {
        if (metric == null) {
            throw new BadRequestException("Metric data is required.");
        }

        // Fetch parent model context
        MachineLearningModel model = InMemoryDatabase.getModels().get(modelId);
        if (model == null) {
            throw new NotFoundException("Parent model with ID '" + modelId + "' not found.");
        }

        // State Constraint (403 Forbidden): If model is DEPRECATED, block metric ingestion
        if (model.getStatus() != null && "DEPRECATED".equalsIgnoreCase(model.getStatus().trim())) {
            throw new ModelDeprecatedException("Cannot add evaluation metrics: Model '" + modelId + "' is DEPRECATED.");
        }

        // Generate a unique ID (UUID) for the metric event
        metric.setId(UUID.randomUUID().toString());

        // Set current timestamp if not provided
        if (metric.getTimestamp() <= 0) {
            metric.setTimestamp(System.currentTimeMillis());
        }

        // Save metric to the model's history list
        List<EvaluationMetric> modelMetrics = InMemoryDatabase.getOrCreateMetricsForModel(modelId);
        modelMetrics.add(metric);

        // Side Effect: Update the latestAccuracy of the parent MachineLearningModel
        model.setLatestAccuracy(metric.getAccuracyScore());

        return Response.status(Response.Status.CREATED).entity(metric).build();
    }
}
