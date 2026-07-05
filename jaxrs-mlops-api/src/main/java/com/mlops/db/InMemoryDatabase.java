package com.mlops.db;

import com.mlops.model.EvaluationMetric;
import com.mlops.model.MachineLearningModel;
import com.mlops.model.MLWorkspace;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryDatabase {
    private static final Map<String, MLWorkspace> workspaces = new ConcurrentHashMap<>();
    private static final Map<String, MachineLearningModel> models = new ConcurrentHashMap<>();
    private static final Map<String, List<EvaluationMetric>> metrics = new ConcurrentHashMap<>();

    public static Map<String, MLWorkspace> getWorkspaces() {
        return workspaces;
    }

    public static Map<String, MachineLearningModel> getModels() {
        return models;
    }

    public static Map<String, List<EvaluationMetric>> getMetrics() {
        return metrics;
    }

    public static List<EvaluationMetric> getOrCreateMetricsForModel(String modelId) {
        return metrics.computeIfAbsent(modelId, k -> new CopyOnWriteArrayList<>());
    }
}
