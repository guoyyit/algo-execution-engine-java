package com.github.guoyyit.algoexecutionenginejava.strategy;

import com.github.guoyyit.algoexecutionenginejava.model.MarketTick;
import com.github.guoyyit.algoexecutionenginejava.model.StrategyType;

import java.util.List;

public class PlanContext {
    private StrategyType strategy;
    private long now;
    private long windowStart;
    private long windowEnd;
    private int sliceCount;
    private double participationRate;
    private long remainingQuantity;
    private int planVersion;
    private MarketTick latestTick;
    private List<MarketTick> historyTicks;

    public StrategyType getStrategy() { return strategy; }
    public void setStrategy(StrategyType strategy) { this.strategy = strategy; }
    public long getNow() { return now; }
    public void setNow(long now) { this.now = now; }
    public long getWindowStart() { return windowStart; }
    public void setWindowStart(long windowStart) { this.windowStart = windowStart; }
    public long getWindowEnd() { return windowEnd; }
    public void setWindowEnd(long windowEnd) { this.windowEnd = windowEnd; }
    public int getSliceCount() { return sliceCount; }
    public void setSliceCount(int sliceCount) { this.sliceCount = sliceCount; }
    public double getParticipationRate() { return participationRate; }
    public void setParticipationRate(double participationRate) { this.participationRate = participationRate; }
    public long getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(long remainingQuantity) { this.remainingQuantity = remainingQuantity; }
    public int getPlanVersion() { return planVersion; }
    public void setPlanVersion(int planVersion) { this.planVersion = planVersion; }
    public MarketTick getLatestTick() { return latestTick; }
    public void setLatestTick(MarketTick latestTick) { this.latestTick = latestTick; }
    public List<MarketTick> getHistoryTicks() { return historyTicks; }
    public void setHistoryTicks(List<MarketTick> historyTicks) { this.historyTicks = historyTicks; }
}
