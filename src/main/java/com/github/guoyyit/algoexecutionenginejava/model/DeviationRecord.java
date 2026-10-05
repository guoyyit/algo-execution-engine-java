package com.github.guoyyit.algoexecutionenginejava.model;

public record DeviationRecord(
        String parentOrderId,
        long timestamp,
        int sliceIndex,
        long plannedCumQty,
        long actualCumQty,
        long deviationQty,
        String reason
) {
}
