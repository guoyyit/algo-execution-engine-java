package com.github.guoyyit.algoexecutionenginejava.model;

public class PlanDeviation {
    private long time;
    private int planVersion;
    private int sliceIndex;
    private long plannedCumQuantity;
    private long actualCumFilled;
    private long quantityDeviation;
    private long plannedTime;
    private long timeDeviationMillis;
    private String reason;

    public PlanDeviation() {
    }

    public long getTime() { return time; }
    public void setTime(long time) { this.time = time; }
    public int getPlanVersion() { return planVersion; }
    public void setPlanVersion(int planVersion) { this.planVersion = planVersion; }
    public int getSliceIndex() { return sliceIndex; }
    public void setSliceIndex(int sliceIndex) { this.sliceIndex = sliceIndex; }
    public long getPlannedCumQuantity() { return plannedCumQuantity; }
    public void setPlannedCumQuantity(long plannedCumQuantity) { this.plannedCumQuantity = plannedCumQuantity; }
    public long getActualCumFilled() { return actualCumFilled; }
    public void setActualCumFilled(long actualCumFilled) { this.actualCumFilled = actualCumFilled; }
    public long getQuantityDeviation() { return quantityDeviation; }
    public void setQuantityDeviation(long quantityDeviation) { this.quantityDeviation = quantityDeviation; }
    public long getPlannedTime() { return plannedTime; }
    public void setPlannedTime(long plannedTime) { this.plannedTime = plannedTime; }
    public long getTimeDeviationMillis() { return timeDeviationMillis; }
    public void setTimeDeviationMillis(long timeDeviationMillis) { this.timeDeviationMillis = timeDeviationMillis; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
