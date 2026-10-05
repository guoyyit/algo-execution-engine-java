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

class ExpiryTest {

    @TempDir
    Path tempDir;

    @Test
    void orderExpiresAtEndTimeWhenAccountBlocksAllSubmissions() {
        EngineProperties properties = EngineTestSupport.properties(tempDir);
        properties.setAccountMaxCash(1L);
        ExecutionEngine engine = EngineTestSupport.newEngine(tempDir, properties);

        OrderRequest request = new OrderRequest("900001", Side.BUY, 1000L, 300L,
                StrategyType.TIME_SCHEDULE, 3, null, 88L);
        ParentOrder parent = engine.submit(request);

        for (int i = 0; i < 10; i++) {
            engine.stepOnce();
        }

        ParentOrder expired = engine.getParent(parent.getId());
        assertEquals(ParentOrderStatus.EXPIRED, expired.getStatus(),
                "parent expires after end time if it cannot complete");
        assertEquals(0L, expired.getFilledQty());
        assertTrue(expired.getStatus().isTerminal());
    }
}
