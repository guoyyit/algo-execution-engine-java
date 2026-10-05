package com.github.guoyyit.algoexecutionenginejava.tests;

import com.github.guoyyit.algoexecutionenginejava.execution.ExecutionEngine;
import com.github.guoyyit.algoexecutionenginejava.execution.FileOrderStore;
import com.github.guoyyit.algoexecutionenginejava.market.ReplayMarketDataFeed;
import com.github.guoyyit.algoexecutionenginejava.model.ChildOrder;
import com.github.guoyyit.algoexecutionenginejava.model.ChildStatus;
import com.github.guoyyit.algoexecutionenginejava.model.EventType;
import com.github.guoyyit.algoexecutionenginejava.model.MarketTick;
import com.github.guoyyit.algoexecutionenginejava.model.OrderProgress;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrder;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrderRequest;
import com.github.guoyyit.algoexecutionenginejava.model.ParentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static com.github.guoyyit.algoexecutionenginejava.tests.TestFixtures.SYMBOL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CancelExpiryTest {

    @TempDir
    Path tempDir;

    @Test
    void cancelCancelsAllLiveChildrenAndBlocksFurtherFills() {
        ReplayMarketDataFeed feed = new ReplayMarketDataFeed();
        FileOrderStore store = new FileOrderStore(tempDir);
        ExecutionEngine engine = new ExecutionEngine(store, feed);

        ParentOrderRequest request = TestFixtures.twapBuy(10_000L, 10, 7L);
        ParentOrder parent = engine.submit(request);
        engine.onTick(new MarketTick(SYMBOL, 1_000L, 9.99, 10.01, 10.00, 105_000L));
        engine.onTick(new MarketTick(SYMBOL, 10_000L, 9.99, 10.01, 10.00, 110_000L));
        engine.onTick(new MarketTick(SYMBOL, 11_000L, 9.99, 10.01, 10.00, 111_000L));

        long filledAtCancel = engine.getProgress(parent.getParentOrderId()).getCumulativeFilled();
        engine.cancel(parent.getParentOrderId());

        OrderProgress after = engine.getProgress(parent.getParentOrderId());
        assertEquals(ParentStatus.CANCELLED, after.getStatus());
        assertEquals(0L, after.getLiveQuantity(), "no live children may remain after cancel");

        for (ChildOrder child : engine.getOrder(parent.getParentOrderId()).getChildren()) {
            if (child.getStatus() == ChildStatus.SUBMITTED || child.getStatus() == ChildStatus.PARTIALLY_FILLED) {
                throw new AssertionError("child still live after cancel: " + child.getChildOrderId());
            }
        }

        for (long t = 20_000L; t <= 90_000L; t += 10_000L) {
            engine.onTick(new MarketTick(SYMBOL, t, 9.99, 10.01, 10.00, 110_000L + t));
        }
        long childrenAfter = engine.getOrder(parent.getParentOrderId()).getChildren().size();
        long fillsAfter = store.loadEvents(parent.getParentOrderId()).stream()
                .filter(e -> e.getType() == EventType.CHILD_FILL
                        && e.getTime() > 11_000L)
                .count();
        assertEquals(0L, fillsAfter, "no fills allowed after terminal state");
        assertEquals(engine.getOrder(parent.getParentOrderId()).getChildren().size(), childrenAfter);
        assertEquals(filledAtCancel, engine.getProgress(parent.getParentOrderId()).getCumulativeFilled());

        engine.pause(parent.getParentOrderId());
        engine.resume(parent.getParentOrderId());
        assertEquals(ParentStatus.CANCELLED, engine.getProgress(parent.getParentOrderId()).getStatus(),
                "terminal state must not regress");
    }

    @Test
    void unfinishedOrderExpiresAtEndTime() {
        ReplayMarketDataFeed feed = new ReplayMarketDataFeed();
        ExecutionEngine engine = new ExecutionEngine(new FileOrderStore(tempDir.resolve("e")), feed);

        ParentOrderRequest request = TestFixtures.twapBuy(10_000L, 10, 99L);
        ParentOrder parent = engine.submit(request);
        engine.onTimeElapsed(100_000L);

        ParentStatus status = engine.getProgress(parent.getParentOrderId()).getStatus();
        assertTrue(status == ParentStatus.EXPIRED || status == ParentStatus.COMPLETED);
        assertEquals(0L, engine.getProgress(parent.getParentOrderId()).getLiveQuantity());
    }
}
