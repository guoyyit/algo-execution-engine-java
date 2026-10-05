package com.github.guoyyit.algoexecutionenginejava.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public class ExecutionPlan {
    private int version;
    private long generatedAt;
    private StrategyType strategy;
    private long windowStart;
    private long windowEnd;
    private long remainingQuantity;
    private List<Slice> slices = new ArrayList<>();

    public ExecutionPlan() {
    }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public long getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(long generatedAt) { this.generatedAt = generatedAt; }
    public StrategyType getStrategy() { return strategy; }
    public void setStrategy(StrategyType strategy) { this.strategy = strategy; }
    public long getWindowStart() { return windowStart; }
    public void setWindowStart(long windowStart) { this.windowStart = windowStart; }
    public long getWindowEnd() { return windowEnd; }
    public void setWindowEnd(long windowEnd) { this.windowEnd = windowEnd; }
    public long getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(long remainingQuantity) { this.remainingQuantity = remainingQuantity; }
    public List<Slice> getSlices() { return slices; }
    public void setSlices(List<Slice> slices) { this.slices = slices; }

    @JsonIgnore
    public long totalPlannedQuantity() {
        long total = 0L;
        for (Slice slice : slices) {
            total += slice.getPlannedQuantity();
        }
        return total;
    }

    @JsonIgnore
    public long plannedCumQuantityByTime(long time) {
        long cum = 0L;
        for (Slice slice : slices) {
            if (slice.getPlannedTime() <= time) {
                cum += slice.getPlannedQuantity();
            } else {
                break;
            }
        }
        return cum;
    }

    @JsonIgnore
    public Slice sliceAt(int index) {
        if (index < 0 || index >= slices.size()) {
            return null;
        }
        return slices.get(index);
    }
}
