package com.github.guoyyit.algoexecutionenginejava.market;

import com.github.guoyyit.algoexecutionenginejava.model.MarketTick;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReplayMarketDataFeed implements MarketDataFeed {

    private final Map<String, List<MarketTick>> histories = new ConcurrentHashMap<>();

    public ReplayMarketDataFeed() {
    }

    public ReplayMarketDataFeed(MarketScenario scenario) {
        if (scenario != null && scenario.getSymbol() != null) {
            histories.put(scenario.getSymbol(), new ArrayList<>(scenario.getTicks()));
        }
    }

    @Override
    public MarketTick latest(String symbol) {
        List<MarketTick> ticks = histories.get(symbol);
        if (ticks == null || ticks.isEmpty()) {
            return null;
        }
        synchronized (ticks) {
            return ticks.get(ticks.size() - 1);
        }
    }

    @Override
    public List<MarketTick> history(String symbol) {
        List<MarketTick> ticks = histories.get(symbol);
        if (ticks == null) {
            return new ArrayList<>();
        }
        synchronized (ticks) {
            return new ArrayList<>(ticks);
        }
    }

    @Override
    public void onTick(MarketTick tick) {
        List<MarketTick> ticks = histories.computeIfAbsent(tick.getSymbol(), key -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (ticks) {
            MarketTick last = ticks.isEmpty() ? null : ticks.get(ticks.size() - 1);
            if (last != null && tick.getTime() < last.getTime()) {
                return;
            }
            ticks.add(tick);
        }
    }

    public MarketTick latestAtOrBefore(String symbol, long time) {
        List<MarketTick> ticks = histories.get(symbol);
        if (ticks == null) {
            return null;
        }
        synchronized (ticks) {
            MarketTick result = null;
            for (MarketTick tick : ticks) {
                if (tick.getTime() <= time) {
                    result = tick;
                } else {
                    break;
                }
            }
            return result;
        }
    }
}
