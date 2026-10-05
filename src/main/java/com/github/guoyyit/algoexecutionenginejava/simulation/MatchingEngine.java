package com.github.guoyyit.algoexecutionenginejava.simulation;

import com.github.guoyyit.algoexecutionenginejava.model.ChildOrder;
import com.github.guoyyit.algoexecutionenginejava.model.ChildOrderStatus;
import com.github.guoyyit.algoexecutionenginejava.model.Fill;
import com.github.guoyyit.algoexecutionenginejava.model.Quote;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MatchingEngine {

    private final SimClock clock;
    private final ConcurrentHashMap<String, RestingOrder> resting = new ConcurrentHashMap<>();

    public MatchingEngine(MarketDataSource marketData, SimClock clock, java.util.Random ignored) {
        this.clock = clock;
    }

    public List<Fill> submit(ChildOrder child, Quote quote) {
        List<Fill> immediate = new ArrayList<>();
        resting.put(child.getId(), new RestingOrder(child, quote.symbol()));
        long initialQty = Math.min(child.remainingQty(),
                fillQty(child, child.remainingQty(), quote, clock.currentTick()));
        child.setSubmittedAt(clock.now());
        child.setLastUpdated(clock.now());
        if (initialQty > 0) {
            immediate.add(applyFill(child, quote.price(), initialQty));
        }
        child.setStatus(child.remainingQty() == 0L
                ? ChildOrderStatus.FILLED : ChildOrderStatus.PARTIALLY_FILLED);
        if (child.getStatus() == ChildOrderStatus.FILLED) {
            resting.remove(child.getId());
        }
        return immediate;
    }

    public List<Fill> onTick(Quote quote) {
        List<Fill> fills = new ArrayList<>();
        if (quote == null) {
            return fills;
        }
        long tick = clock.currentTick();
        for (RestingOrder order : new ArrayList<>(resting.values())) {
            if (!order.symbol.equals(quote.symbol())) {
                continue;
            }
            ChildOrder child = order.child;
            synchronized (child) {
                if (!child.getStatus().isActive()) {
                    resting.remove(child.getId());
                    continue;
                }
                long qty = Math.min(child.remainingQty(),
                        fillQty(child, child.remainingQty(), quote, tick));
                if (qty > 0) {
                    fills.add(applyFill(child, quote.price(), qty));
                }
                if (child.remainingQty() == 0L) {
                    child.setStatus(ChildOrderStatus.FILLED);
                    resting.remove(child.getId());
                }
            }
        }
        return fills;
    }

    public long cancel(ChildOrder child) {
        synchronized (child) {
            if (!child.getStatus().isActive()) {
                return 0L;
            }
            long cancelledQty = child.remainingQty();
            resting.remove(child.getId());
            child.setStatus(ChildOrderStatus.CANCELLED);
            child.setLastUpdated(clock.now());
            return cancelledQty;
        }
    }

    public void restoreResting(ChildOrder child, String symbol) {
        if (child.getStatus().isActive()) {
            resting.put(child.getId(), new RestingOrder(child,
                    symbol != null ? symbol : child.getSymbol()));
        }
    }

    public boolean isResting(String childOrderId) {
        return resting.containsKey(childOrderId);
    }

    private long fillQty(ChildOrder child, long remaining, Quote quote, long tick) {
        int pct = deterministicPercent(child.getId(), tick, quote.sequence());
        pct = Math.max(pct, 15);
        long qty = remaining * pct / 100L;
        if (pct > 0 && qty == 0L) {
            qty = 1L;
        }
        return Math.min(qty, remaining);
    }

    private int deterministicPercent(String childId, long tick, long quoteSequence) {
        long h = tick * 0x9E3779B97F4A7C15L;
        h ^= quoteSequence * 0xC2B2AE3D27D4EB4FL;
        h ^= stringHash(childId);
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 31;
        return (int) (Math.floorMod(h, 101L));
    }

    private long stringHash(String value) {
        long h = 1125899906842597L;
        for (int i = 0; i < value.length(); i++) {
            h = 31L * h + value.charAt(i);
        }
        return h;
    }

    private Fill applyFill(ChildOrder child, long price, long qty) {
        long newFilled = child.getFilledQty() + qty;
        long totalNotional = child.getAvgPrice() * child.getFilledQty() + price * qty;
        child.setFilledQty(newFilled);
        child.setAvgPrice(totalNotional / newFilled);
        child.setLastUpdated(clock.now());
        long sequence = clock.currentTick() * 1_000_000L + stringHash(child.getId()) % 1_000_000L;
        return new Fill(child.getId(), child.getParentOrderId(), qty, price, clock.now(), sequence);
    }

    private record RestingOrder(ChildOrder child, String symbol) {
    }
}
