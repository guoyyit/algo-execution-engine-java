package com.github.guoyyit.algoexecutionenginejava.tests;

import com.github.guoyyit.algoexecutionenginejava.config.EngineProperties;
import com.github.guoyyit.algoexecutionenginejava.engine.ExecutionEngine;
import com.github.guoyyit.algoexecutionenginejava.model.ChildOrderStatus;
import com.github.guoyyit.algoexecutionenginejava.model.ExecutionPlan;
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

class PauseResumeTest {

    @TempDir
    Path tempDir;

    @Test
    void pauseStopsSlicesAndResumeRebuildsPlanForRemainingQty() {
        EngineProperties properties = EngineTestSupport.properties(tempDir);
        ExecutionEngine engine = EngineTestSupport.newEngine(tempDir, properties);

        OrderRequest request = new OrderRequest("600001", Side.BUY, 1000L, 400L,
                StrategyType.TIME_SCHEDULE, 4, null, 7L);
        ParentOrder parent = engine.submit(request);

        engine.stepOnce();
        long childrenAfterFirstTick = engine.store().childrenOf(parent.getId()).size();
        assertTrue(childrenAfterFirstTick >= 1);

        engine.pause(parent.getId());
        long childrenAtPause = engine.store().childrenOf(parent.getId()).size();
        assertEquals(ParentOrderStatus.PAUSED, engine.getParent(parent.getId()).getStatus());
        for (var child : engine.store().childrenOf(parent.getId())) {
            assertTrue(child.getStatus() == ChildOrderStatus.CANCELLED
                            || child.getStatus() == ChildOrderStatus.FILLED,
                    "in-flight child must be cancelled or filled after pause");
        }

        for (int i = 0; i < 5; i++) {
            engine.stepOnce();
        }
        assertEquals(childrenAtPause, engine.store().childrenOf(parent.getId()).size(),
                "no new children while paused");
        assertEquals(ParentOrderStatus.PAUSED, engine.getParent(parent.getId()).getStatus());

        long filledAtPause = engine.getParent(parent.getId()).getFilledQty();
        ExecutionPlan planBefore = engine.store().latestPlan(parent.getId()).orElseThrow();

        engine.resume(parent.getId());
        assertEquals(ParentOrderStatus.ACTIVE, engine.getParent(parent.getId()).getStatus());

        ExecutionPlan planAfter = engine.store().latestPlan(parent.getId()).orElseThrow();
        assertEquals(planBefore.getVersion() + 1, planAfter.getVersion());
        assertEquals(1000L - filledAtPause, planAfter.totalPlannedQty(),
                "new plan covers exactly the remaining quantity");

        for (int i = 0; i < 40; i++) {
            engine.stepOnce();
        }

        ParentOrder finished = engine.getParent(parent.getId());
        assertEquals(ParentOrderStatus.COMPLETED, finished.getStatus());
        assertEquals(1000L, finished.getFilledQty());
    }
}
