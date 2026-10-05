package com.github.guoyyit.algoexecutionenginejava.model;

public class Slice {
    private int index;
    private long plannedTime;
    private long plannedQuantity;
    private long plannedCumVolume;

    public Slice() {
    }

    public Slice(int index, long plannedTime, long plannedQuantity, long plannedCumVolume) {
        this.index = index;
        this.plannedTime = plannedTime;
        this.plannedQuantity = plannedQuantity;
        this.plannedCumVolume = plannedCumVolume;
    }

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }
    public long getPlannedTime() { return plannedTime; }
    public void setPlannedTime(long plannedTime) { this.plannedTime = plannedTime; }
    public long getPlannedQuantity() { return plannedQuantity; }
    public void setPlannedQuantity(long plannedQuantity) { this.plannedQuantity = plannedQuantity; }
    public long getPlannedCumVolume() { return plannedCumVolume; }
    public void setPlannedCumVolume(long plannedCumVolume) { this.plannedCumVolume = plannedCumVolume; }
}
