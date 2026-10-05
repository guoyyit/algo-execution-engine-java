package com.github.guoyyit.algoexecutionenginejava.tests;

import com.github.guoyyit.algoexecutionenginejava.execution.ExecutionEngine;
import com.github.guoyyit.algoexecutionenginejava.execution.FileOrderStore;
import com.github.guoyyit.algoexecutionenginejava.market.ReplayMarketDataFeed;
import com.github.guoyyit.algoexecutionenginejava.model.ChildOrder;
import com.github.guoyyit.algoexecutionenginejava.model.ChildStatus;
import com.github.guoyyit.algoexecutionenginejava.model.MarketTick;
import com.github.guoyyit.algoexecutionenginejava.model.OrderProgress;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrder;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrderRequest;
import com.github.guoyyit.algoexecutionenginejava.model.PlanDeviation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static com.github.guoyyit.algoexecutionenginejava.tests.TestFixtures.SYMBOL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartialFillAndDeviationTest {

    @TempDir
    Path tempDir;

    @Test
    void unfilledQuantityReturnsToRemainingAndDeviationsAreRecordedUniformly() {
        ReplayMarketDataFeed feed = new ReplayMarketDataFeed();
        FileOrderStore store = new FileOrderStore(tempDir);
        ExecutionEngine engine = new ExecutionEngine(store, feed);

        ParentOrderRequest request = TestFixtures.twapBuy(10_000L, 10, 888L);
        ParentOrder parent = engine.submit(request);

        for (long t = 1_000L; t <= 60_000L; t += 1_000L) {
            engine.onTick(new MarketTick(SYMBOL, t, 9.99, 10.01, 10.00, 100_000L + t * 4));
        }

        ParentOrder snapshot = engine.getOrder(parent.getParentOrderId());
        boolean sawPartialOrCancelled = false;
        for (ChildOrder child : snapshot.getChildren()) {
            if (child.getStatus() == ChildStatus.PARTIALLY_FILLED
                    || child.getStatus() == ChildStatus.CANCELLED
                    || child.getStatus() == ChildStatus.EXPIRED) {
                sawPartialOrCancelled = true;
            }
            assertTrue(child.getFilledQuantity() <= child.getPlannedQuantity());
        }
        assertTrue(sawPartialOrCancelled, "simulation should produce partial/cancelled child paths");

        OrderProgress progress = engine.getProgress(parent.getParentOrderId());
        assertEquals(10_000L,
                progress.getCumulativeFilled() + progress.getRemainingQuantity(),
                "unfilled quantity must stay in remaining task quantity");

        assertTrue(progress.getDeviationQuantity() >= 0L || progress.getStatus().isTerminal());
        for (PlanDeviation deviation : snapshot.getDeviations()) {
            assertEquals(deviation.getPlannedCumQuantity() - deviation.getActualCumFilled(),
                    deviation.getQuantityDeviation(),
                    "deviation must use the uniform definition plannedCum - actualCumFilled");
            assertEquals(deviation.getPlanVersion(), progress.getPlanVersion());
        }
    }
}
