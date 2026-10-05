package com.github.guoyyit.algoexecutionenginejava.model;

public class OrderProgress {
    private String parentOrderId;
    private String clientOrderId;
    private ParentStatus status;
    private String symbol;
    private Side side;
    private long totalQuantity;
    private long cumulativeFilled;
    private long remainingQuantity;
    private long liveQuantity;
    private double progressRatio;
    private double averageFillPrice;
    private long plannedCumQuantity;
    private long deviationQuantity;
    private int planVersion;
    private String faultReason;

    public String getParentOrderId() { return parentOrderId; }
    public void setParentOrderId(String parentOrderId) { this.parentOrderId = parentOrderId; }
    public String getClientOrderId() { return clientOrderId; }
    public void setClientOrderId(String clientOrderId) { this.clientOrderId = clientOrderId; }
    public ParentStatus getStatus() { return status; }
    public void setStatus(ParentStatus status) { this.status = status; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public Side getSide() { return side; }
    public void setSide(Side side) { this.side = side; }
    public long getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(long totalQuantity) { this.totalQuantity = totalQuantity; }
    public long getCumulativeFilled() { return cumulativeFilled; }
    public void setCumulativeFilled(long cumulativeFilled) { this.cumulativeFilled = cumulativeFilled; }
    public long getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(long remainingQuantity) { this.remainingQuantity = remainingQuantity; }
    public long getLiveQuantity() { return liveQuantity; }
    public void setLiveQuantity(long liveQuantity) { this.liveQuantity = liveQuantity; }
    public double getProgressRatio() { return progressRatio; }
    public void setProgressRatio(double progressRatio) { this.progressRatio = progressRatio; }
    public double getAverageFillPrice() { return averageFillPrice; }
    public void setAverageFillPrice(double averageFillPrice) { this.averageFillPrice = averageFillPrice; }
    public long getPlannedCumQuantity() { return plannedCumQuantity; }
    public void setPlannedCumQuantity(long plannedCumQuantity) { this.plannedCumQuantity = plannedCumQuantity; }
    public long getDeviationQuantity() { return deviationQuantity; }
    public void setDeviationQuantity(long deviationQuantity) { this.deviationQuantity = deviationQuantity; }
    public int getPlanVersion() { return planVersion; }
    public void setPlanVersion(int planVersion) { this.planVersion = planVersion; }
    public String getFaultReason() { return faultReason; }
    public void setFaultReason(String faultReason) { this.faultReason = faultReason; }
}
