package com.github.guoyyit.algoexecutionenginejava.tests;

import com.github.guoyyit.algoexecutionenginejava.config.EngineProperties;
import com.github.guoyyit.algoexecutionenginejava.engine.ExecutionEngine;
import com.github.guoyyit.algoexecutionenginejava.model.DeviationRecord;
import com.github.guoyyit.algoexecutionenginejava.model.OrderRequest;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrder;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrderStatus;
import com.github.guoyyit.algoexecutionenginejava.model.Side;
import com.github.guoyyit.algoexecutionenginejava.model.StrategyType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExceptionPathTest {

    @TempDir
    Path tempDir;

    @Test
    void pausedOrderRecordsExplainableStaleQuoteDeviationAndStopsNewSlices() {
        EngineProperties properties = EngineTestSupport.properties(tempDir);
        properties.setStaleQuoteAction("PAUSE");
        ExecutionEngine engine = EngineTestSupport.newEngine(tempDir, properties);

        OrderRequest request = new OrderRequest("800001", Side.BUY, 1000L, 2000L,
                StrategyType.TIME_SCHEDULE, 10, null, 55L);
        ParentOrder parent = engine.submit(request);
        engine.stepOnce();

        long childrenBeforeFreeze = engine.store().childrenOf(parent.getId()).size();

        engine.marketData().setFrozen("800001", true);
        for (int i = 0; i < 4; i++) {
            engine.stepOnce();
        }
        long childrenAtStale = engine.store().childrenOf(parent.getId()).size();
        for (int i = 0; i < 10; i++) {
            engine.stepOnce();
        }

        assertEquals(ParentOrderStatus.PAUSED, engine.getParent(parent.getId()).getStatus());
        assertEquals(childrenAtStale, engine.store().childrenOf(parent.getId()).size(),
                "stale quote pauses and prevents new child orders");

        engine.marketData().setFrozen("800001", false);
        engine.resume(parent.getId());
        for (int i = 0; i < 60; i++) {
            engine.stepOnce();
        }

        ParentOrder finished = engine.getParent(parent.getId());
        assertTrue(finished.getStatus() == ParentOrderStatus.COMPLETED
                || finished.getStatus() == ParentOrderStatus.EXPIRED);
        assertEquals(1000L, finished.getFilledQty(),
                "unfilled quantity returns to remaining and eventually fills");
    }

    @Test
    void degradeActionKeepsOrderActiveButDoesNotSubmitWhileStale() {
        EngineProperties properties = EngineTestSupport.properties(tempDir);
        properties.setStaleQuoteAction("DEGRADE");
        ExecutionEngine engine = EngineTestSupport.newEngine(tempDir, properties);

        OrderRequest request = new OrderRequest("800002", Side.BUY, 1000L, 2000L,
                StrategyType.TIME_SCHEDULE, 10, null, 56L);
        ParentOrder parent = engine.submit(request);
        engine.stepOnce();

        long childrenBeforeFreeze = engine.store().childrenOf(parent.getId()).size();
        engine.marketData().setFrozen("800002", true);
        for (int i = 0; i < 4; i++) {
            engine.stepOnce();
        }
        long childrenAtStale = engine.store().childrenOf(parent.getId()).size();
        for (int i = 0; i < 10; i++) {
            engine.stepOnce();
        }

        assertEquals(ParentOrderStatus.ACTIVE, engine.getParent(parent.getId()).getStatus(),
                "DEGRADE keeps parent ACTIVE");
        assertEquals(childrenAtStale, engine.store().childrenOf(parent.getId()).size(),
                "no new submissions during degraded window");

        engine.marketData().setFrozen("800002", false);
        for (int i = 0; i < 60; i++) {
            engine.stepOnce();
        }
        assertEquals(1000L, engine.getParent(parent.getId()).getFilledQty());
    }

    @Test
    void deviationsUseSinglePlannedVersusFilledDefinition() {
        EngineProperties properties = EngineTestSupport.properties(tempDir);
        ExecutionEngine engine = EngineTestSupport.newEngine(tempDir, properties);

        OrderRequest request = new OrderRequest("800003", Side.BUY, 1000L, 1000L,
                StrategyType.TIME_SCHEDULE, 10, null, 57L);
        ParentOrder parent = engine.submit(request);
        for (int i = 0; i < 15; i++) {
            engine.stepOnce();
        }

        assertFalse(engine.store().deviations(parent.getId()).isEmpty());
        for (DeviationRecord deviation : engine.store().deviations(parent.getId())) {
            assertEquals(deviation.actualCumQty() - deviation.plannedCumQty(),
                    deviation.deviationQty(),
                    "deviation is uniformly defined as actualFilled - plannedCum");
            assertEquals(parent.getId(), deviation.parentOrderId());
        }
    }
}
