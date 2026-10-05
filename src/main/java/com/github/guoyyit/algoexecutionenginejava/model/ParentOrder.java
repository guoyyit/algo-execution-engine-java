package com.github.guoyyit.algoexecutionenginejava.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public class ParentOrder {
    private String parentOrderId;
    private String clientOrderId;
    private String symbol;
    private Side side;
    private long totalQuantity;
    private StrategyType strategy;
    private long startTime;
    private long endTime;
    private int sliceCount;
    private double participationRate;
    private StaleQuotePolicy staleQuotePolicy;
    private long staleQuoteTimeoutMillis;
    private long randomSeed;

    private ParentStatus status;
    private long cumulativeFilled;
    private double averageFillPrice;
    private int nextSliceIndex;
    private long createdAt;
    private long lastUpdateTime;
    private String faultReason;

    private ExecutionPlan currentPlan;
    private List<ChildOrder> children = new ArrayList<>();
    private List<PlanDeviation> deviations = new ArrayList<>();

    public ParentOrder() {
    }

    public String getParentOrderId() { return parentOrderId; }
    public void setParentOrderId(String parentOrderId) { this.parentOrderId = parentOrderId; }
    public String getClientOrderId() { return clientOrderId; }
    public void setClientOrderId(String clientOrderId) { this.clientOrderId = clientOrderId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public Side getSide() { return side; }
    public void setSide(Side side) { this.side = side; }
    public long getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(long totalQuantity) { this.totalQuantity = totalQuantity; }
    public StrategyType getStrategy() { return strategy; }
    public void setStrategy(StrategyType strategy) { this.strategy = strategy; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public int getSliceCount() { return sliceCount; }
    public void setSliceCount(int sliceCount) { this.sliceCount = sliceCount; }
    public double getParticipationRate() { return participationRate; }
    public void setParticipationRate(double participationRate) { this.participationRate = participationRate; }
    public StaleQuotePolicy getStaleQuotePolicy() { return staleQuotePolicy; }
    public void setStaleQuotePolicy(StaleQuotePolicy staleQuotePolicy) { this.staleQuotePolicy = staleQuotePolicy; }
    public long getStaleQuoteTimeoutMillis() { return staleQuoteTimeoutMillis; }
    public void setStaleQuoteTimeoutMillis(long staleQuoteTimeoutMillis) { this.staleQuoteTimeoutMillis = staleQuoteTimeoutMillis; }
    public long getRandomSeed() { return randomSeed; }
    public void setRandomSeed(long randomSeed) { this.randomSeed = randomSeed; }
    public ParentStatus getStatus() { return status; }
    public void setStatus(ParentStatus status) { this.status = status; }
    public long getCumulativeFilled() { return cumulativeFilled; }
    public void setCumulativeFilled(long cumulativeFilled) { this.cumulativeFilled = cumulativeFilled; }
    public int getNextSliceIndex() { return nextSliceIndex; }
    public void setNextSliceIndex(int nextSliceIndex) { this.nextSliceIndex = nextSliceIndex; }
    public double getAverageFillPrice() { return averageFillPrice; }
    public void setAverageFillPrice(double averageFillPrice) { this.averageFillPrice = averageFillPrice; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getLastUpdateTime() { return lastUpdateTime; }
    public void setLastUpdateTime(long lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
    public String getFaultReason() { return faultReason; }
    public void setFaultReason(String faultReason) { this.faultReason = faultReason; }
    public ExecutionPlan getCurrentPlan() { return currentPlan; }
    public void setCurrentPlan(ExecutionPlan currentPlan) { this.currentPlan = currentPlan; }
    public List<ChildOrder> getChildren() { return children; }
    public void setChildren(List<ChildOrder> children) { this.children = children; }
    public List<PlanDeviation> getDeviations() { return deviations; }
    public void setDeviations(List<PlanDeviation> deviations) { this.deviations = deviations; }

    @JsonIgnore
    public long getOutstandingQuantity() {
        return Math.max(0L, totalQuantity - cumulativeFilled);
    }

    @JsonIgnore
    public long getLiveQuantity() {
        long live = 0L;
        for (ChildOrder child : children) {
            if (child.isLive()) {
                live += child.getOutstandingQuantity();
            }
        }
        return live;
    }

    @JsonIgnore
    public long getPlannedUnsubmittedQuantity() {
        return Math.max(0L, getOutstandingQuantity() - getLiveQuantity());
    }

    public ChildOrder findChild(String childOrderId) {
        for (ChildOrder child : children) {
            if (child.getChildOrderId().equals(childOrderId)) {
                return child;
            }
        }
        return null;
    }

    public void applyChildFill(ChildOrder child, long quantity, double price, long time) {
        child.applyFill(quantity, price, time);
        long newFilled = cumulativeFilled + quantity;
        averageFillPrice = ((averageFillPrice * cumulativeFilled) + (price * quantity)) / newFilled;
        cumulativeFilled = newFilled;
        lastUpdateTime = time;
    }
}
