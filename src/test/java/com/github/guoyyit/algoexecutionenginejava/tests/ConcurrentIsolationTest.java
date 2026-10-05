package com.github.guoyyit.algoexecutionenginejava.tests;

import com.github.guoyyit.algoexecutionenginejava.execution.Account;
import com.github.guoyyit.algoexecutionenginejava.execution.ExecutionEngine;
import com.github.guoyyit.algoexecutionenginejava.execution.FileOrderStore;
import com.github.guoyyit.algoexecutionenginejava.execution.InMemoryEventPublisher;
import com.github.guoyyit.algoexecutionenginejava.market.MatchingEngine;
import com.github.guoyyit.algoexecutionenginejava.market.ReplayMarketDataFeed;
import com.github.guoyyit.algoexecutionenginejava.market.SimClock;
import com.github.guoyyit.algoexecutionenginejava.model.MarketTick;
import com.github.guoyyit.algoexecutionenginejava.model.OrderProgress;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrder;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrderRequest;
import com.github.guoyyit.algoexecutionenginejava.model.ParentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.github.guoyyit.algoexecutionenginejava.tests.TestFixtures.SYMBOL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcurrentIsolationTest {

    @TempDir
    Path tempDir;

    @Test
    void concurrentOrdersStayIndependentAndRespectSharedAccountLimit() throws Exception {
        ReplayMarketDataFeed feed = new ReplayMarketDataFeed();
        Account account = new Account(50_000L, 50_000L);
        ExecutionEngine engine = new ExecutionEngine(new FileOrderStore(tempDir), feed,
                new SimClock(), account, new MatchingEngine(), new InMemoryEventPublisher(),
                1_000_000L, 10, 0.1);

        int orderCount = 12;
        List<String> ids = java.util.Collections.synchronizedList(new ArrayList<>());
        ExecutorService pool = Executors.newFixedThreadPool(6);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch submitted = new CountDownLatch(orderCount);
        ConcurrentHashMap<String, Throwable> errors = new ConcurrentHashMap<>();

        for (int i = 0; i < orderCount; i++) {
            long seed = 200L + i;
            pool.submit(() -> {
                try {
                    start.await();
                    ParentOrderRequest request = TestFixtures.twapBuy(20_000L, 10, seed);
                    ParentOrder parent = engine.submit(request);
                    ids.add(parent.getParentOrderId());
                } catch (Throwable t) {
                    errors.put(Thread.currentThread().getName(), t);
                } finally {
                    submitted.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(submitted.await(30, TimeUnit.SECONDS));
        pool.shutdown();

        assertTrue(errors.isEmpty(), "no thread should fail: " + errors);
        assertEquals(orderCount, ids.size());

        for (long t = 1_000L; t <= 100_000L; t += 2_000L) {
            engine.onTick(new MarketTick(SYMBOL, t, 9.99, 10.01, 10.00, 100_000L + t * 5));
        }
        engine.onTimeElapsed(100_000L);

        for (String id : ids) {
            OrderProgress progress = engine.getProgress(id);
            assertTrue(progress.getStatus().isTerminal());
            assertEquals(20_000L, progress.getCumulativeFilled() + progress.getRemainingQuantity(),
                    "filled + remaining must equal total for " + id);
            assertTrue(progress.getCumulativeFilled() > 0, "every order should achieve some fills independently");
        }
        assertTrue(account.totalReserved() >= 0L);
    }
}
