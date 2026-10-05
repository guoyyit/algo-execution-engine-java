package com.github.guoyyit.algoexecutionenginejava.tests;

import com.github.guoyyit.algoexecutionenginejava.market.MarketScenario;
import com.github.guoyyit.algoexecutionenginejava.model.MarketTick;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrderRequest;
import com.github.guoyyit.algoexecutionenginejava.model.Side;
import com.github.guoyyit.algoexecutionenginejava.model.StaleQuotePolicy;
import com.github.guoyyit.algoexecutionenginejava.model.StrategyType;

public final class TestFixtures {

    public static final String SYMBOL = "600000.SH";

    private TestFixtures() {
    }

    public static ParentOrderRequest twapBuy(long quantity, int sliceCount, long seed) {
        ParentOrderRequest request = new ParentOrderRequest();
        request.setClientOrderId("client-" + seed);
        request.setSymbol(SYMBOL);
        request.setSide(Side.BUY);
        request.setTotalQuantity(quantity);
        request.setStrategy(StrategyType.TIME_WEIGHTED);
        request.setStartTime(0L);
        request.setEndTime(100_000L);
        request.setSliceCount(sliceCount);
        request.setStaleQuotePolicy(StaleQuotePolicy.PAUSE);
        request.setStaleQuoteTimeoutMillis(5_000L);
        request.setRandomSeed(seed);
        return request;
    }

    public static ParentOrderRequest volumeBuy(long quantity, int sliceCount, double participation, long seed) {
        ParentOrderRequest request = twapBuy(quantity, sliceCount, seed);
        request.setStrategy(StrategyType.VOLUME_PARTICIPATION);
        request.setParticipationRate(participation);
        return request;
    }

    public static MarketScenario smoothTicks(int ticks, long intervalMillis, long stepVolume) {
        MarketScenario scenario = new MarketScenario();
        scenario.setSymbol(SYMBOL);
        long cumVolume = 100_000L;
        for (int i = 0; i < ticks; i++) {
            long time = (i + 1L) * intervalMillis;
            double mid = 10.0 + i * 0.001;
            cumVolume += stepVolume;
            scenario.getTicks().add(new MarketTick(SYMBOL, time, mid - 0.01, mid + 0.01, mid, cumVolume));
        }
        return scenario;
    }

    public static MarketScenario gappedTicks(long gapStart, long gapLength, long intervalMillis) {
        MarketScenario scenario = new MarketScenario();
        scenario.setSymbol(SYMBOL);
        long cumVolume = 100_000L;
        long time = intervalMillis;
        for (int i = 0; i < 20; i++) {
            if (i == gapStart) {
                time += gapLength;
            } else {
                time += intervalMillis;
            }
            double mid = 10.0 + i * 0.001;
            cumVolume += 5_000L;
            scenario.getTicks().add(new MarketTick(SYMBOL, time, mid - 0.01, mid + 0.01, mid, cumVolume));
        }
        return scenario;
    }
}
