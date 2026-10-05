package com.github.guoyyit.algoexecutionenginejava.market;

public class SimClock {

    private long now;

    public SimClock() {
        this.now = 0L;
    }

    public SimClock(long start) {
        this.now = start;
    }

    public long millis() {
        return now;
    }

    public void setTime(long time) {
        if (time < now) {
            throw new IllegalArgumentException("clock cannot move backwards: " + time + " < " + now);
        }
        this.now = time;
    }

    public void advance(long deltaMillis) {
        if (deltaMillis < 0) {
            throw new IllegalArgumentException("delta must be non-negative");
        }
        this.now += deltaMillis;
    }
}
