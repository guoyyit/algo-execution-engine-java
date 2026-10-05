package com.github.guoyyit.algoexecutionenginejava.tests;

import com.github.guoyyit.algoexecutionenginejava.execution.ExecutionEngine;
import com.github.guoyyit.algoexecutionenginejava.execution.FileOrderStore;
import com.github.guoyyit.algoexecutionenginejava.market.ReplayMarketDataFeed;
import com.github.guoyyit.algoexecutionenginejava.model.EventType;
import com.github.guoyyit.algoexecutionenginejava.model.ExecutionEvent;
import com.github.guoyyit.algoexecutionenginejava.model.MarketTick;
import com.github.guoyyit.algoexecutionenginejava.model.OrderProgress;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrder;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrderRequest;
import com.github.guoyyit.algoexecutionenginejava.model.ParentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static com.github.guoyyit.algoexecutionenginejava.tests.TestFixtures.SYMBOL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PauseResumeTest {

    @TempDir
    Path tempDir;

    @Test
    void noNewChildrenWhilePausedAndPlanRegeneratedForRemainingOnResume() {
        ReplayMarketDataFeed feed = new ReplayMarketDataFeed();
        FileOrderStore store = new FileOrderStore(tempDir);
        ExecutionEngine engine = new ExecutionEngine(store, feed);

        ParentOrderRequest request = TestFixtures.twapBuy(10_000L, 10, 42L);
        ParentOrder parent = engine.submit(request);

        engine.onTick(new MarketTick(SYMBOL, 1_000L, 9.99, 10.01, 10.00, 105_000L));
        engine.onTick(new MarketTick(SYMBOL, 5_000L, 9.99, 10.01, 10.00, 108_000L));
        engine.onTick(new MarketTick(SYMBOL, 10_000L, 9.99, 10.01, 10.00, 110_000L));
        engine.onTick(new MarketTick(SYMBOL, 11_000L, 9.99, 10.01, 10.00, 111_000L));

        engine.pause(parent.getParentOrderId());
        OrderProgress paused = engine.getProgress(parent.getParentOrderId());
        assertEquals(ParentStatus.PAUSED, paused.getStatus());
        int childCountAtPause = engine.getOrder(parent.getParentOrderId()).getChildren().size();

        for (long t = 12_000L; t <= 16_000L; t += 2_000L) {
            engine.onTick(new MarketTick(SYMBOL, t, 9.99, 10.01, 10.00, 110_000L + t));
        }
        int childCountAfterTicks = engine.getOrder(parent.getParentOrderId()).getChildren().size();
        assertEquals(childCountAtPause, childCountAfterTicks, "paused order must not spawn children");

        engine.resume(parent.getParentOrderId());
        assertEquals(ParentStatus.ACTIVE, engine.getProgress(parent.getParentOrderId()).getStatus());

        List<ExecutionEvent> events = store.loadEvents(parent.getParentOrderId());
        long planVersions = events.stream().filter(e -> e.getType() == EventType.PLAN_GENERATED).count();
        assertTrue(planVersions >= 2, "resume must regenerate plan");

        long filledBeforeResume = events.stream()
                .filter(e -> e.getType() == EventType.CHILD_FILL)
                .mapToLong(ExecutionEvent::getQuantity).sum();
        List<ExecutionEvent> planEvents = events.stream()
                .filter(e -> e.getType() == EventType.PLAN_GENERATED).toList();
        long regeneratedPlanQty = planEvents.get(planEvents.size() - 1).getQuantity();
        assertEquals(10_000L - filledBeforeResume, regeneratedPlanQty,
                "regenerated plan must cover exactly the unfilled remaining quantity");

        engine.onTimeElapsed(100_000L);
        OrderProgress end = engine.getProgress(parent.getParentOrderId());
        assertTrue(end.getStatus().isTerminal());
        assertEquals(10_000L, end.getCumulativeFilled() + end.getRemainingQuantity(),
                "filled + remaining must stay consistent with total");
    }
}
