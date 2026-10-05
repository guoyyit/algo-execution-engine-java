package com.github.guoyyit.algoexecutionenginejava.tests;

import com.github.guoyyit.algoexecutionenginejava.config.EngineProperties;
import com.github.guoyyit.algoexecutionenginejava.engine.ExecutionEngine;
import com.github.guoyyit.algoexecutionenginejava.model.ChildOrderStatus;
import com.github.guoyyit.algoexecutionenginejava.model.OrderRequest;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrder;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrderStatus;
import com.github.guoyyit.algoexecutionenginejava.model.Side;
import com.github.guoyyit.algoexecutionenginejava.model.StrategyType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CancelTest {

    @TempDir
    Path tempDir;

    @Test
    void cancelCancelsInFlightChildrenAndParentBecomesTerminal() {
        EngineProperties properties = EngineTestSupport.properties(tempDir);
        ExecutionEngine engine = EngineTestSupport.newEngine(tempDir, properties);

        OrderRequest request = new OrderRequest("600002", Side.BUY, 1000L, 1000L,
                StrategyType.TIME_SCHEDULE, 10, null, 9L);
        ParentOrder parent = engine.submit(request);
        engine.stepOnce();

        engine.cancel(parent.getId());
        for (int i = 0; i < 3; i++) {
            engine.stepOnce();
        }

        ParentOrder cancelled = engine.getParent(parent.getId());
        assertEquals(ParentOrderStatus.CANCELLED, cancelled.getStatus());

        long childrenAtCancel = engine.store().childrenOf(parent.getId()).size();
        long filledAtCancel = cancelled.getFilledQty();

        for (var child : engine.store().childrenOf(parent.getId())) {
            assertTrue(child.getStatus() == ChildOrderStatus.CANCELLED
                    || child.getStatus() == ChildOrderStatus.FILLED);
            assertTrue(!child.getStatus().isActive());
        }

        for (int i = 0; i < 10; i++) {
            engine.stepOnce();
        }

        assertEquals(childrenAtCancel, engine.store().childrenOf(parent.getId()).size(),
                "no new children after terminal state");
        assertEquals(filledAtCancel, engine.getParent(parent.getId()).getFilledQty(),
                "no new fills after terminal state");
        assertEquals(ParentOrderStatus.CANCELLED,
                engine.getParent(parent.getId()).getStatus(), "status never regresses");

        engine.resume(parent.getId());
        assertEquals(ParentOrderStatus.CANCELLED, engine.getParent(parent.getId()).getStatus(),
                "resume on terminal order is ignored");
    }
}
