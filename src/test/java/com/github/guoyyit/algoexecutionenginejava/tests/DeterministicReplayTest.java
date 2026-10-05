package com.github.guoyyit.algoexecutionenginejava.tests;

import com.github.guoyyit.algoexecutionenginejava.market.MarketScenario;
import com.github.guoyyit.algoexecutionenginejava.model.ExecutionEvent;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrderRequest;
import com.github.guoyyit.algoexecutionenginejava.replay.ReplayRequest;
import com.github.guoyyit.algoexecutionenginejava.replay.ReplayResult;
import com.github.guoyyit.algoexecutionenginejava.replay.ReplayRunner;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeterministicReplayTest {

    private final ReplayRunner runner = new ReplayRunner();

    @Test
    void sameSeedAndSameTicksProduceIdenticalTrace() {
        MarketScenario scenario = TestFixtures.smoothTicks(50, 2_000L, 8_000L);

        ReplayResult first = runner.run(request(777L), scenario);
        ReplayResult second = runner.run(request(777L), scenario);

        assertFalse(first.getEvents().isEmpty());
        assertEquals(first.getEvents().size(), second.getEvents().size());
        assertTrue(ReplayRunner.sameTrace(first, second),
                "execution must be bit-for-bit reproducible for the same seed and tick series");

        List<ExecutionEvent> fills = first.getEvents().stream()
                .filter(e -> e.getType().name().equals("CHILD_FILL")).toList();
        assertTrue(fills.size() > 1, "partial fills should occur across slices");
    }

    @Test
    void differentSeedsProduceDifferentTrace() {
        MarketScenario scenario = TestFixtures.smoothTicks(50, 2_000L, 8_000L);
        ReplayResult first = runner.run(request(1L), scenario);
        ReplayResult second = runner.run(request(2L), scenario);
        assertFalse(ReplayRunner.sameTrace(first, second),
                "a different random seed must be allowed to change fills");
    }

    @Test
    void volumeStrategyIsReplayableDeterministically() {
        MarketScenario scenario = TestFixtures.smoothTicks(50, 2_000L, 8_000L);
        ParentOrderRequest request = TestFixtures.volumeBuy(20_000L, 10, 0.15, 321L);
        ReplayRequest replayRequest = new ReplayRequest();
        replayRequest.setRandomSeed(321L);
        replayRequest.setOrderRequest(request);
        replayRequest.setScenario(scenario);

        ReplayResult first = runner.run(replayRequest, scenario);
        ReplayResult second = runner.run(request(321L, false), scenario);
        assertTrue(ReplayRunner.sameTrace(first, second));
    }

    private ReplayRequest request(long seed) {
        return request(seed, true);
    }

    private ReplayRequest request(long seed, boolean twap) {
        ParentOrderRequest order = twap
                ? TestFixtures.twapBuy(10_000L, 10, seed)
                : TestFixtures.volumeBuy(20_000L, 10, 0.15, seed);
        ReplayRequest request = new ReplayRequest();
        request.setRandomSeed(seed);
        request.setOrderRequest(order);
        return request;
    }
}
