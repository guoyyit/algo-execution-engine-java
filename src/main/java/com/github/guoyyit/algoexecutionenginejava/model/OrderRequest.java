package com.github.guoyyit.algoexecutionenginejava.model;

public record OrderRequest(
        String symbol,
        Side side,
        long totalQty,
        long durationMillis,
        StrategyType strategy,
        Integer sliceCount,
        Double participationRate,
        Long seed
) {
}
