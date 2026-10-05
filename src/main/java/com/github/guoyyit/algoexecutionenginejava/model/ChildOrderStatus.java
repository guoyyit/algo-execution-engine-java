package com.github.guoyyit.algoexecutionenginejava.model;

public enum ChildOrderStatus {
    PENDING,
    SUBMITTED,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED;

    public boolean isTerminal() {
        return this == FILLED || this == CANCELLED;
    }

    public boolean isActive() {
        return this == SUBMITTED || this == PARTIALLY_FILLED;
    }
}
