package com.github.guoyyit.algoexecutionenginejava.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public enum ParentStatus {
    CREATED,
    ACTIVE,
    PAUSED,
    COMPLETED,
    CANCELLED,
    EXPIRED,
    FAILED;

    @JsonIgnore
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == EXPIRED || this == FAILED;
    }
}
