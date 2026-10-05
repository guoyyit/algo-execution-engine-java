package com.github.guoyyit.algoexecutionenginejava.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.guoyyit.algoexecutionenginejava.config.EngineProperties;
import com.github.guoyyit.algoexecutionenginejava.engine.ExecutionEngine;
import com.github.guoyyit.algoexecutionenginejava.model.ChildOrder;
import com.github.guoyyit.algoexecutionenginejava.model.OrderRequest;
import com.github.guoyyit.algoexecutionenginejava.model.Side;
import com.github.guoyyit.algoexecutionenginejava.model.StrategyType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeterminismReplayTest {

    @TempDir
    Path tempDirA;

    @TempDir
    Path tempDirB;

    @Test
    void sameSeedsProduceIdenticalFillSequences() {
        Trace traceA = run(tempDirA, StrategyType.TIME_SCHEDULE);
        Trace traceB = run(tempDirB, StrategyType.TIME_SCHEDULE);
        assertEquals(traceA.report(), traceB.report());
    }

    @Test
    void volumeStrategyIsAlsoDeterministic() {
        Trace traceA = run(tempDirA, StrategyType.VOLUME_PARTICIPATION);
        Trace traceB = run(tempDirB, StrategyType.VOLUME_PARTICIPATION);
        assertEquals(traceA.report(), traceB.report());
    }

    private Trace run(Path dir, StrategyType strategy) {
        EngineProperties properties = EngineTestSupport.properties(dir);
        ExecutionEngine engine = EngineTestSupport.newEngine(dir, properties);
        OrderRequest request = new OrderRequest("700000", Side.SELL, 500L, 600L,
                strategy, 5, 0.2d, 12345L);
        var parent = engine.submit(request);
        for (int i = 0; i < 20; i++) {
            engine.stepOnce();
        }
        Trace trace = new Trace();
        trace.parentStatus = engine.getParent(parent.getId()).getStatus().name();
        trace.parentFilled = engine.getParent(parent.getId()).getFilledQty();
        ObjectMapper mapper = new ObjectMapper();
        for (ChildOrder child : engine.store().childrenOf(parent.getId())) {
            trace.children.add(child.getStatus().name() + ":" + child.getQty()
                    + ":" + child.getFilledQty() + ":" + child.getAvgPrice());
        }
        engine.store().deviations(parent.getId()).forEach(deviation ->
                trace.deviations.add(deviation.sliceIndex() + ":" + deviation.plannedCumQty()
                        + ":" + deviation.actualCumQty() + ":" + deviation.deviationQty()));
        return trace;
    }

    private static final class Trace {
        private String parentStatus;
        private long parentFilled;
        private final List<String> children = new java.util.ArrayList<>();
        private final List<String> deviations = new java.util.ArrayList<>();

        private String report() {
            return parentStatus + "|" + parentFilled + "|" + children + "|" + deviations;
        }
    }
}
