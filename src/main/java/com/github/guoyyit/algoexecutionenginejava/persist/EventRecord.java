package com.github.guoyyit.algoexecutionenginejava.persist;

public record EventRecord(
        long sequence,
        long timestamp,
        String type,
        String payloadJson
) {
}
