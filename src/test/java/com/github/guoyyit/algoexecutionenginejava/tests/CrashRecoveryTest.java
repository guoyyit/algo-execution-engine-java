package com.github.guoyyit.algoexecutionenginejava.tests;

import com.github.guoyyit.algoexecutionenginejava.config.EngineProperties;
import com.github.guoyyit.algoexecutionenginejava.engine.ExecutionEngine;
import com.github.guoyyit.algoexecutionenginejava.model.ChildOrder;
import com.github.guoyyit.algoexecutionenginejava.model.ChildOrderStatus;
import com.github.guoyyit.algoexecutionenginejava.model.OrderRequest;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrder;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrderStatus;
import com.github.guoyyit.algoexecutionenginejava.model.Side;
import com.github.guoyyit.algoexecutionenginejava.model.StrategyType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrashRecoveryTest {

    @TempDir
    Path tempDir;

    @Test
    void rebuildsStateFromEventLogAndContinuesWithoutDuplicateSubmissions() {
        EngineProperties properties = EngineTestSupport.properties(tempDir);
        ExecutionEngine first = EngineTestSupport.newEngine(tempDir, properties);

        OrderRequest request = new OrderRequest("600003", Side.BUY, 1000L, 2000L,
                StrategyType.TIME_SCHEDULE, 10, null, 11L);
        ParentOrder parent = first.submit(request);
        String parentId = parent.getId();

        for (int i = 0; i < 5; i++) {
            first.stepOnce();
        }

        long filledBeforeCrash = first.getParent(parentId).getFilledQty();
        List<String> childIdsBefore = first.store().childrenOf(parentId).stream()
                .map(ChildOrder::getId).sorted().toList();
        assertTrue(filledBeforeCrash > 0 || !childIdsBefore.isEmpty());

        ExecutionEngine second = EngineTestSupport.newEngine(tempDir, properties);

        ParentOrder reloaded = second.getParent(parentId);
        assertNotNull(reloaded);
        assertEquals(filledBeforeCrash, reloaded.getFilledQty());
        List<String> childIdsAfterRebuild = second.store().childrenOf(parentId).stream()
                .map(ChildOrder::getId).sorted().toList();
        assertEquals(childIdsBefore, childIdsAfterRebuild,
                "no child order is duplicated or lost on recovery");

        for (ChildOrder child : second.store().childrenOf(parentId)) {
            if (child.getStatus().isActive()) {
                assertTrue(second.matchingEngine().isResting(child.getId()),
                        "active child is restored into the matching book");
            }
        }

        long childrenAtRecovery = second.store().childrenOf(parentId).size();

        for (int i = 0; i < 100; i++) {
            second.stepOnce();
        }

        ParentOrder finished = second.getParent(parentId);
        assertTrue(finished.getStatus() == ParentOrderStatus.COMPLETED
                        || finished.getStatus() == ParentOrderStatus.EXPIRED,
                "recovered parent reaches a terminal state");
        assertEquals(1000L, finished.getFilledQty(), "all quantity eventually filled");

        List<String> allChildIds = second.store().childrenOf(parentId).stream()
                .map(ChildOrder::getId).sorted().toList();
        assertEquals(allChildIds.stream().distinct().count(), allChildIds.size(),
                "no duplicate child ids");
        assertTrue(second.store().childrenOf(parentId).size() >= childrenAtRecovery);

        for (ChildOrder child : second.store().childrenOf(parentId)) {
            assertTrue(child.getStatus() == ChildOrderStatus.FILLED
                            || child.getStatus() == ChildOrderStatus.CANCELLED);
        }
    }
}
