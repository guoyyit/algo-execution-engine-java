package com.github.guoyyit.algoexecutionenginejava.model;

public class ChildOrder {

    private String id;
    private String parentOrderId;
    private String symbol;
    private int sliceIndex;
    private int planVersion;
    private long qty;
    private long filledQty;
    private long avgPrice;
    private long submittedTick;
    private ChildOrderStatus status;
    private long submittedAt;
    private long lastUpdated;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentOrderId() {
        return parentOrderId;
    }

    public void setParentOrderId(String parentOrderId) {
        this.parentOrderId = parentOrderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getSliceIndex() {
        return sliceIndex;
    }

    public void setSliceIndex(int sliceIndex) {
        this.sliceIndex = sliceIndex;
    }

    public int getPlanVersion() {
        return planVersion;
    }

    public void setPlanVersion(int planVersion) {
        this.planVersion = planVersion;
    }

    public long getQty() {
        return qty;
    }

    public void setQty(long qty) {
        this.qty = qty;
    }

    public long getFilledQty() {
        return filledQty;
    }

    public void setFilledQty(long filledQty) {
        this.filledQty = filledQty;
    }

    public long getAvgPrice() {
        return avgPrice;
    }

    public void setAvgPrice(long avgPrice) {
        this.avgPrice = avgPrice;
    }

    public long getSubmittedTick() {
        return submittedTick;
    }

    public void setSubmittedTick(long submittedTick) {
        this.submittedTick = submittedTick;
    }

    public ChildOrderStatus getStatus() {
        return status;
    }

    public void setStatus(ChildOrderStatus status) {
        this.status = status;
    }

    public long getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(long submittedAt) {
        this.submittedAt = submittedAt;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public long remainingQty() {
        return qty - filledQty;
    }
}
