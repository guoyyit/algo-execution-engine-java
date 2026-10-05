package com.github.guoyyit.algoexecutionenginejava.simulation;

public class TickClock implements SimClock {

    private final long tickIntervalMillis;
    private long tickCount;

    public TickClock(long tickIntervalMillis) {
        this.tickIntervalMillis = tickIntervalMillis;
        this.tickCount = 0L;
    }

    @Override
    public long now() {
        return tickCount * tickIntervalMillis;
    }

    @Override
    public long tick() {
        tickCount++;
        return now();
    }

    @Override
    public long currentTick() {
        return tickCount;
    }

    @Override
    public void restoreTick(long tickCount) {
        this.tickCount = Math.max(this.tickCount, tickCount);
    }
}
