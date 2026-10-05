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

class VolumeStrategyTest {

    @TempDir
    Path tempDir;

    @Test
    void volumeParticipationPlanTracksMarketVolumeMilestones() {
        EngineProperties properties = EngineTestSupport.properties(tempDir);
        ExecutionEngine engine = EngineTestSupport.newEngine(tempDir, properties);

        OrderRequest request = new OrderRequest("600004", Side.BUY, 500L, 5000L,
                StrategyType.VOLUME_PARTICIPATION, 5, 0.5d, 77L);
        ParentOrder parent = engine.submit(request);

        var plan = engine.store().latestPlan(parent.getId()).orElseThrow();
        assertEquals(5, plan.getSlices().size());
        assertEquals(500L, plan.totalPlannedQty());

        for (int i = 0; i < 80; i++) {
            engine.stepOnce();
        }

        ParentOrder finished = engine.getParent(parent.getId());
        assertTrue(finished.getStatus() == ParentOrderStatus.COMPLETED
                || finished.getStatus() == ParentOrderStatus.EXPIRED);
        assertEquals(500L, finished.getFilledQty());
    }
}
