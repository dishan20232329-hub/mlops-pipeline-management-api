package com.mlops.model;

public class MachineLearningModel {
    private String id;
    private String framework;
    private String status;
    private double latestAccuracy;
    private String workspaceId;

    public MachineLearningModel() {
    }

    public MachineLearningModel(String id, String framework, String status, double latestAccuracy, String workspaceId) {
        this.id = id;
        this.framework = framework;
        this.status = status;
        this.latestAccuracy = latestAccuracy;
        this.workspaceId = workspaceId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFramework() {
        return framework;
    }

    public void setFramework(String framework) {
        this.framework = framework;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getLatestAccuracy() {
        return latestAccuracy;
    }

    public void setLatestAccuracy(double latestAccuracy) {
        this.latestAccuracy = latestAccuracy;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }
}
