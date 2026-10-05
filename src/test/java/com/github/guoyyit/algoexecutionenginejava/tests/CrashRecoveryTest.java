package com.github.guoyyit.algoexecutionenginejava.tests;

import com.github.guoyyit.algoexecutionenginejava.execution.ExecutionEngine;
import com.github.guoyyit.algoexecutionenginejava.execution.FileOrderStore;
import com.github.guoyyit.algoexecutionenginejava.market.ReplayMarketDataFeed;
import com.github.guoyyit.algoexecutionenginejava.model.ChildOrder;
import com.github.guoyyit.algoexecutionenginejava.model.MarketTick;
import com.github.guoyyit.algoexecutionenginejava.model.OrderProgress;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrder;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrderRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static com.github.guoyyit.algoexecutionenginejava.tests.TestFixtures.SYMBOL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrashRecoveryTest {

    @TempDir
    Path tempDir;

    @Test
    void rebuildsStateFromStoreAndDoesNotResubmitKnownChildren() {
        ReplayMarketDataFeed feed = new ReplayMarketDataFeed();
        FileOrderStore store = new FileOrderStore(tempDir);

        ParentOrderRequest request = TestFixtures.twapBuy(10_000L, 10, 123L);
        String parentId;
        long filledBeforeCrash;
        List<String> childIdsBeforeCrash;

        {
            ExecutionEngine engine = new ExecutionEngine(store, feed);
            ParentOrder parent = engine.submit(request);
            parentId = parent.getParentOrderId();
            engine.onTick(new MarketTick(SYMBOL, 1_000L, 9.99, 10.01, 10.00, 105_000L));
            engine.onTick(new MarketTick(SYMBOL, 10_000L, 9.99, 10.01, 10.00, 110_000L));
            engine.onTick(new MarketTick(SYMBOL, 11_000L, 9.99, 10.01, 10.00, 111_000L));
            filledBeforeCrash = engine.getProgress(parentId).getCumulativeFilled();
            childIdsBeforeCrash = engine.getOrder(parentId).getChildren().stream()
                    .map(ChildOrder::getChildOrderId).toList();
        }

        ReplayMarketDataFeed recoveredFeed = new ReplayMarketDataFeed();
        recoveredFeed.onTick(new MarketTick(SYMBOL, 11_000L, 9.99, 10.01, 10.00, 111_000L));
        ExecutionEngine recovered = new ExecutionEngine(store, recoveredFeed);
        recovered.recover();

        OrderProgress progress = recovered.getProgress(parentId);
        assertEquals(filledBeforeCrash, progress.getCumulativeFilled(),
                "cumulative fill must be rebuilt from persistent records");
        assertEquals(10_000L, progress.getCumulativeFilled() + progress.getRemainingQuantity());

        List<String> childIdsAfterRecovery = recovered.getOrder(parentId).getChildren().stream()
                .map(ChildOrder::getChildOrderId).toList();
        assertEquals(childIdsBeforeCrash.size(), childIdsAfterRecovery.size());
        assertTrue(childIdsAfterRecovery.containsAll(childIdsBeforeCrash));

        recovered.onTick(new MarketTick(SYMBOL, 20_000L, 9.99, 10.01, 10.00, 115_000L));
        List<String> childIdsAfterMoreTicks = recovered.getOrder(parentId).getChildren().stream()
                .map(ChildOrder::getChildOrderId).toList();
        for (String id : childIdsBeforeCrash) {
            assertEquals(1L, childIdsAfterMoreTicks.stream().filter(id::equals).count(),
                    "known child order must not be resubmitted after recovery");
        }

        recovered.onTimeElapsed(100_000L);
        OrderProgress end = recovered.getProgress(parentId);
        assertTrue(end.getStatus().isTerminal());
        assertEquals(10_000L, end.getCumulativeFilled() + end.getRemainingQuantity());
    }
}
