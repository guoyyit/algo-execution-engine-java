package com.github.guoyyit.algoexecutionenginejava.model;

public enum ParentOrderStatus {
    NEW,
    ACTIVE,
    PAUSED,
    CANCELLING,
    CANCELLED,
    COMPLETED,
    EXPIRED;

    public boolean isTerminal() {
        return this == CANCELLED || this == COMPLETED || this == EXPIRED;
    }
}
