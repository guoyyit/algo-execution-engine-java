package com.github.guoyyit.algoexecutionenginejava.model;

public record Fill(
        String childOrderId,
        String parentOrderId,
        long qty,
        long price,
        long timestamp,
        long sequence
) {
}
