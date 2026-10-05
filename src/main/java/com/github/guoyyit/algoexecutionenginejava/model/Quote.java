package com.github.guoyyit.algoexecutionenginejava.model;

public record Quote(
        String symbol,
        long price,
        long cumulativeMarketVolume,
        long timestamp,
        long sequence
) {
}
