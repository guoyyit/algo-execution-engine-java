package com.github.guoyyit.algoexecutionenginejava.model;

public record PlanSlice(
        int index,
        long scheduledTime,
        long plannedQty
) {
}
