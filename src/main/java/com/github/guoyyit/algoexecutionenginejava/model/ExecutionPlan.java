package com.github.guoyyit.algoexecutionenginejava.model;

import java.util.List;

public class ExecutionPlan {

    private String parentOrderId;
    private int version;
    private long createdAt;
    private List<PlanSlice> slices;

    public ExecutionPlan() {
    }

    public ExecutionPlan(String parentOrderId, int version, long createdAt, List<PlanSlice> slices) {
        this.parentOrderId = parentOrderId;
        this.version = version;
        this.createdAt = createdAt;
        this.slices = slices;
    }

    public String getParentOrderId() {
        return parentOrderId;
    }

    public void setParentOrderId(String parentOrderId) {
        this.parentOrderId = parentOrderId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public List<PlanSlice> getSlices() {
        return slices;
    }

    public void setSlices(List<PlanSlice> slices) {
        this.slices = slices;
    }

    public long totalPlannedQty() {
        return slices == null ? 0L : slices.stream().mapToLong(PlanSlice::plannedQty).sum();
    }
}
