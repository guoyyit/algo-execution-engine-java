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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcurrencyIsolationTest {

    @TempDir
    Path tempDir;

    @Test
    void multipleParentsExecuteConcurrentlyWithoutInterference() throws Exception {
        EngineProperties properties = EngineTestSupport.properties(tempDir);
        ExecutionEngine engine = EngineTestSupport.newEngine(tempDir, properties);

        int orderCount = 8;
        List<String> parentIds = new ArrayList<>();
        for (int i = 0; i < orderCount; i++) {
            OrderRequest request = new OrderRequest("SYM" + i, Side.BUY, 1000L, 800L,
                    StrategyType.TIME_SCHEDULE, 4, null, (long) (100 + i));
            parentIds.add(engine.submit(request).getId());
        }

        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch done = new CountDownLatch(orderCount);
        for (String id : parentIds) {
            pool.submit(() -> {
                try {
                    for (int tick = 0; tick < 30; tick++) {
                        synchronized (engine) {
                            engine.stepOnce();
                        }
                    }
                } finally {
                    done.countDown();
                }
            });
        }
        assertTrue(done.await(30, TimeUnit.SECONDS));
        pool.shutdown();

        for (String id : parentIds) {
            ParentOrder parent = engine.getParent(id);
            assertTrue(parent.getStatus() == ParentOrderStatus.COMPLETED,
                    id + " expected COMPLETED but " + parent.getStatus());
            assertEquals(1000L, parent.getFilledQty(), id + " filled full qty");
        }
    }

    @Test
    void accountCapRejectionDoesNotBlockOtherParents() {
        EngineProperties properties = EngineTestSupport.properties(tempDir);
        properties.setAccountMaxPosition(500L);
        ExecutionEngine engine = EngineTestSupport.newEngine(tempDir, properties);

        OrderRequest blocked = new OrderRequest("BIG", Side.BUY, 10_000L, 1000L,
                StrategyType.TIME_SCHEDULE, 2, null, 1L);
        ParentOrder blockedParent = engine.submit(blocked);

        OrderRequest normal = new OrderRequest("SMALL", Side.BUY, 100L, 400L,
                StrategyType.TIME_SCHEDULE, 2, null, 2L);
        ParentOrder normalParent = engine.submit(normal);

        for (int i = 0; i < 30; i++) {
            engine.stepOnce();
        }

        ParentOrder normalFinished = engine.getParent(normalParent.getId());
        assertEquals(ParentOrderStatus.COMPLETED, normalFinished.getStatus(),
                "unrelated parent completes even when another is constrained by account cap");
        assertEquals(100L, normalFinished.getFilledQty());
        assertTrue(engine.getParent(blockedParent.getId()).getFilledQty() <= 500L,
                "position cap never exceeded");
    }
}
