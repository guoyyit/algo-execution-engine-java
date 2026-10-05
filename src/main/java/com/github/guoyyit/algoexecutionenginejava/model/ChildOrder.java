package com.github.guoyyit.algoexecutionenginejava.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public class ChildOrder {
    private String childOrderId;
    private String parentOrderId;
    private String symbol;
    private Side side;
    private long plannedQuantity;
    private long filledQuantity;
    private double averagePrice;
    private ChildStatus status;
    private int planVersion;
    private int sliceIndex;
    private long plannedTime;
    private long submittedAt;
    private long expireAt;
    private long lastUpdateTime;

    public ChildOrder() {
    }

    public String getChildOrderId() { return childOrderId; }
    public void setChildOrderId(String childOrderId) { this.childOrderId = childOrderId; }
    public String getParentOrderId() { return parentOrderId; }
    public void setParentOrderId(String parentOrderId) { this.parentOrderId = parentOrderId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public Side getSide() { return side; }
    public void setSide(Side side) { this.side = side; }
    public long getPlannedQuantity() { return plannedQuantity; }
    public void setPlannedQuantity(long plannedQuantity) { this.plannedQuantity = plannedQuantity; }
    public long getFilledQuantity() { return filledQuantity; }
    public void setFilledQuantity(long filledQuantity) { this.filledQuantity = filledQuantity; }
    public double getAveragePrice() { return averagePrice; }
    public void setAveragePrice(double averagePrice) { this.averagePrice = averagePrice; }
    public ChildStatus getStatus() { return status; }
    public void setStatus(ChildStatus status) { this.status = status; }
    public int getPlanVersion() { return planVersion; }
    public void setPlanVersion(int planVersion) { this.planVersion = planVersion; }
    public int getSliceIndex() { return sliceIndex; }
    public void setSliceIndex(int sliceIndex) { this.sliceIndex = sliceIndex; }
    public long getPlannedTime() { return plannedTime; }
    public void setPlannedTime(long plannedTime) { this.plannedTime = plannedTime; }
    public long getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(long submittedAt) { this.submittedAt = submittedAt; }
    public long getExpireAt() { return expireAt; }
    public void setExpireAt(long expireAt) { this.expireAt = expireAt; }
    public long getLastUpdateTime() { return lastUpdateTime; }
    public void setLastUpdateTime(long lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }

    @JsonIgnore
    public long getOutstandingQuantity() {
        return plannedQuantity - filledQuantity;
    }

    @JsonIgnore
    public boolean isLive() {
        return status != null && status.isLive();
    }

    public void applyFill(long quantity, double price, long time) {
        if (quantity <= 0 || quantity > getOutstandingQuantity()) {
            throw new IllegalArgumentException("invalid fill quantity: " + quantity);
        }
        long newFilled = filledQuantity + quantity;
        averagePrice = ((averagePrice * filledQuantity) + (price * quantity)) / newFilled;
        filledQuantity = newFilled;
        status = filledQuantity >= plannedQuantity ? ChildStatus.FILLED : ChildStatus.PARTIALLY_FILLED;
        lastUpdateTime = time;
    }
}
