package com.github.guoyyit.algoexecutionenginejava.execution;

import com.github.guoyyit.algoexecutionenginejava.model.Side;

import java.util.HashMap;
import java.util.Map;

public class Account {

    private final long cashLimit;
    private final long positionLimit;
    private long cashReserved;
    private long positionReserved;
    private final Map<String, long[]> reservations = new HashMap<>();

    public Account() {
        this(Long.MAX_VALUE, Long.MAX_VALUE);
    }

    public Account(long cashLimit, long positionLimit) {
        this.cashLimit = cashLimit;
        this.positionLimit = positionLimit;
    }

    public synchronized boolean tryReserve(String parentOrderId, Side side, long quantity) {
        if (quantity <= 0) {
            return false;
        }
        if (side == Side.BUY) {
            if (cashReserved + quantity > cashLimit) {
                return false;
            }
            cashReserved += quantity;
        } else {
            if (positionReserved + quantity > positionLimit) {
                return false;
            }
            positionReserved += quantity;
        }
        reservations.computeIfAbsent(parentOrderId, key -> new long[2])[side == Side.BUY ? 0 : 1] += quantity;
        return true;
    }

    public synchronized void release(String parentOrderId, Side side, long quantity) {
        long[] entry = reservations.get(parentOrderId);
        int idx = side == Side.BUY ? 0 : 1;
        long releasable = entry == null ? 0L : entry[idx];
        long actual = Math.min(quantity, releasable);
        if (actual <= 0) {
            return;
        }
        entry[idx] -= actual;
        if (side == Side.BUY) {
            cashReserved -= actual;
        } else {
            positionReserved -= actual;
        }
    }

    public synchronized long reserved(String parentOrderId) {
        long[] entry = reservations.get(parentOrderId);
        return entry == null ? 0L : entry[0] + entry[1];
    }

    public synchronized long totalReserved() {
        return cashReserved + positionReserved;
    }

    public synchronized long cashReserved() {
        return cashReserved;
    }

    public synchronized long positionReserved() {
        return positionReserved;
    }
}
