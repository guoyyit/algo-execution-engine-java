package com.github.guoyyit.algoexecutionenginejava.model;

public class ParentOrder {

    private String id;
    private String symbol;
    private Side side;
    private long totalQty;
    private long filledQty;
    private long submittedQty;
    private ParentOrderStatus status;
    private long startTime;
    private long endTime;
    private StrategyType strategy;
    private int sliceCount;
    private double participationRate;
    private long seed;
    private int planVersion;
    private long createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    public long getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(long totalQty) {
        this.totalQty = totalQty;
    }

    public long getFilledQty() {
        return filledQty;
    }

    public void setFilledQty(long filledQty) {
        this.filledQty = filledQty;
    }

    public long getSubmittedQty() {
        return submittedQty;
    }

    public void setSubmittedQty(long submittedQty) {
        this.submittedQty = submittedQty;
    }

    public ParentOrderStatus getStatus() {
        return status;
    }

    public void setStatus(ParentOrderStatus status) {
        this.status = status;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public StrategyType getStrategy() {
        return strategy;
    }

    public void setStrategy(StrategyType strategy) {
        this.strategy = strategy;
    }

    public int getSliceCount() {
        return sliceCount;
    }

    public void setSliceCount(int sliceCount) {
        this.sliceCount = sliceCount;
    }

    public double getParticipationRate() {
        return participationRate;
    }

    public void setParticipationRate(double participationRate) {
        this.participationRate = participationRate;
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public int getPlanVersion() {
        return planVersion;
    }

    public void setPlanVersion(int planVersion) {
        this.planVersion = planVersion;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long remainingQty() {
        return totalQty - filledQty;
    }

    public double progress() {
        return totalQty <= 0 ? 0.0 : (double) filledQty / totalQty;
    }
}
