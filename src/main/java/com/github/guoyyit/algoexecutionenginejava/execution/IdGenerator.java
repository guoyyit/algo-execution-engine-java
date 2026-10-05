package com.github.guoyyit.algoexecutionenginejava.execution;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class IdGenerator {

    private final AtomicLong eventSeq;
    private final AtomicLong parentSeq = new AtomicLong();
    private final ConcurrentHashMap<String, AtomicLong> childSeq = new ConcurrentHashMap<>();

    public IdGenerator() {
        this(0L, 0L);
    }

    public IdGenerator(long parentStart, long eventStart) {
        parentSeq.set(parentStart);
        this.eventSeq = new AtomicLong(eventStart);
    }

    public String nextParentId() {
        return "P-" + parentSeq.incrementAndGet();
    }

    public String nextChildId(String parentOrderId) {
        long seq = childSeq.computeIfAbsent(parentOrderId, key -> new AtomicLong()).incrementAndGet();
        return parentOrderId + "-C-" + seq;
    }

    public long nextEventSequence() {
        return eventSeq.incrementAndGet();
    }

    public void advanceChildSeqTo(String parentOrderId, long value) {
        childSeq.computeIfAbsent(parentOrderId, key -> new AtomicLong())
                .updateAndGet(current -> Math.max(current, value));
    }

    public void advanceParentSeqTo(long value) {
        parentSeq.updateAndGet(current -> Math.max(current, value));
    }
}
