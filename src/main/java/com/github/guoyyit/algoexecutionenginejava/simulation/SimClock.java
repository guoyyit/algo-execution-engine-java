package com.github.guoyyit.algoexecutionenginejava.simulation;

public interface SimClock {

    long now();

    long tick();

    long currentTick();

    void restoreTick(long tickCount);
}
