package com.github.guoyyit.algoexecutionenginejava.tests;

import com.github.guoyyit.algoexecutionenginejava.config.EngineProperties;
import com.github.guoyyit.algoexecutionenginejava.engine.ExecutionEngine;
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

class TimeStrategySmokeTest {

    @TempDir
    Path tempDir;

    @Test
    void timeScheduleParentCompletesAndFillsWholeQuantity() {
        EngineProperties properties = EngineTestSupport.properties(tempDir);
        ExecutionEngine engine = EngineTestSupport.newEngine(tempDir, properties);

        OrderRequest request = new OrderRequest("600000", Side.BUY, 1000L, 400L,
                StrategyType.TIME_SCHEDULE, 4, null, 7L);
        ParentOrder parent = engine.submit(request);

        for (int i = 0; i < 30; i++) {
            engine.stepOnce();
        }

        ParentOrder reloaded = engine.getParent(parent.getId());
        assertTrue(reloaded.getStatus() == ParentOrderStatus.COMPLETED,
                "expected COMPLETED but was " + reloaded.getStatus());
        assertEquals(1000L, reloaded.getFilledQty());
        assertEquals(4, engine.store().childrenOf(parent.getId()).size());
    }
}
