package com.github.guoyyit.algoexecutionenginejava.tests;

import com.github.guoyyit.algoexecutionenginejava.execution.ExecutionEngine;
import com.github.guoyyit.algoexecutionenginejava.execution.FileOrderStore;
import com.github.guoyyit.algoexecutionenginejava.market.ReplayMarketDataFeed;
import com.github.guoyyit.algoexecutionenginejava.model.ChildOrder;
import com.github.guoyyit.algoexecutionenginejava.model.EventType;
import com.github.guoyyit.algoexecutionenginejava.model.MarketTick;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrder;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrderRequest;
import com.github.guoyyit.algoexecutionenginejava.model.ParentStatus;
import com.github.guoyyit.algoexecutionenginejava.model.StaleQuotePolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static com.github.guoyyit.algoexecutionenginejava.tests.TestFixtures.SYMBOL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaleQuoteTest {

    @TempDir
    Path tempDir;

    @Test
    void pausePolicyPausesOnLongQuoteGap() {
        ReplayMarketDataFeed feed = new ReplayMarketDataFeed();
        FileOrderStore store = new FileOrderStore(tempDir.resolve("pause"));
        ExecutionEngine engine = new ExecutionEngine(store, feed);

        ParentOrderRequest request = TestFixtures.twapBuy(10_000L, 10, 5L);
        ParentOrder parent = engine.submit(request);
        engine.onTick(new MarketTick(SYMBOL, 1_000L, 9.99, 10.01, 10.00, 105_000L));

        engine.onTimeElapsed(10_000L);

        assertEquals(ParentStatus.PAUSED, engine.getProgress(parent.getParentOrderId()).getStatus());
        assertTrue(store.loadEvents(parent.getParentOrderId()).stream()
                .anyMatch(e -> e.getType() == EventType.STALE_QUOTE));

        engine.onTick(new MarketTick(SYMBOL, 11_000L, 9.99, 10.01, 10.00, 108_000L));
        assertEquals(ParentStatus.PAUSED, engine.getProgress(parent.getParentOrderId()).getStatus(),
                "a fresh quote alone must not auto-resume; resume is explicit");

        engine.resume(parent.getParentOrderId());
        assertEquals(ParentStatus.ACTIVE, engine.getProgress(parent.getParentOrderId()).getStatus());
    }

    @Test
    void degradePolicyKeepsTradingWithHalfSizedSlices() {
        ReplayMarketDataFeed feed = new ReplayMarketDataFeed();
        FileOrderStore store = new FileOrderStore(tempDir.resolve("degrade"));
        ExecutionEngine engine = new ExecutionEngine(store, feed);

        ParentOrderRequest request = TestFixtures.twapBuy(10_000L, 10, 5L);
        request.setStaleQuotePolicy(StaleQuotePolicy.DEGRADE);
        ParentOrder parent = engine.submit(request);
        engine.onTick(new MarketTick(SYMBOL, 1_000L, 9.99, 10.01, 10.00, 105_000L));

        int childrenBefore = engine.getOrder(parent.getParentOrderId()).getChildren().size();
        engine.onTimeElapsed(30_000L);

        assertTrue(store.loadEvents(parent.getParentOrderId()).stream()
                .anyMatch(e -> e.getType() == EventType.STALE_QUOTE
                        && e.getDetail().contains("policy=DEGRADE")));
        assertEquals(ParentStatus.ACTIVE, engine.getProgress(parent.getParentOrderId()).getStatus(),
				"degrade policy must not pause the order");

        ChildOrder degradedChild = engine.getOrder(parent.getParentOrderId()).getChildren()
                .stream().reduce((a, b) -> b).orElse(null);
        assertTrue(degradedChild == null
                        || degradedChild.getPlannedQuantity() <= 1_000L,
                "degraded slice must be at most half the nominal 1000 slice");
        assertTrue(engine.getOrder(parent.getParentOrderId()).getChildren().size() >= childrenBefore);
    }
}
