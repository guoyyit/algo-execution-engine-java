package com.github.guoyyit.algoexecutionenginejava.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public enum ChildStatus {
    PENDING,
    SUBMITTED,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED,
    EXPIRED,
    REJECTED;

    @JsonIgnore
    public boolean isLive() {
        return this == SUBMITTED || this == PARTIALLY_FILLED;
    }

    @JsonIgnore
    public boolean isTerminal() {
        return this == FILLED || this == CANCELLED || this == EXPIRED || this == REJECTED;
    }
}
