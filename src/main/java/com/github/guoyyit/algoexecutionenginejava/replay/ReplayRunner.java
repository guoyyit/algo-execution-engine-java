package com.github.guoyyit.algoexecutionenginejava.replay;

import com.github.guoyyit.algoexecutionenginejava.execution.ExecutionEngine;
import com.github.guoyyit.algoexecutionenginejava.execution.InMemoryEventPublisher;
import com.github.guoyyit.algoexecutionenginejava.execution.Account;
import com.github.guoyyit.algoexecutionenginejava.execution.OrderStore;
import com.github.guoyyit.algoexecutionenginejava.market.MarketScenario;
import com.github.guoyyit.algoexecutionenginejava.market.MatchingEngine;
import com.github.guoyyit.algoexecutionenginejava.market.ReplayMarketDataFeed;
import com.github.guoyyit.algoexecutionenginejava.market.SimClock;
import com.github.guoyyit.algoexecutionenginejava.model.ExecutionEvent;
import com.github.guoyyit.algoexecutionenginejava.model.MarketTick;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrder;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrderRequest;

import java.util.ArrayList;
import java.util.List;

public class ReplayRunner {

    public ReplayResult run(ReplayRequest request, MarketScenario scenario) {
        return run(request, scenario, new InMemoryOrderStore());
    }

    public ReplayResult run(ReplayRequest request, MarketScenario scenario, OrderStore store) {
        ReplayMarketDataFeed feed = new ReplayMarketDataFeed(scenario);
        SimClock clock = new SimClock(request.getOrderRequest().getStartTime());
        InMemoryEventPublisher publisher = new InMemoryEventPublisher();
        ExecutionEngine engine = new ExecutionEngine(store, feed, clock,
                new Account(Long.MAX_VALUE, Long.MAX_VALUE),
                new MatchingEngine(), publisher,
                request.getOrderRequest().getStaleQuoteTimeoutMillis() > 0
                        ? request.getOrderRequest().getStaleQuoteTimeoutMillis() : 30_000L,
                10, 0.1);

        ParentOrderRequest normalized = normalize(request);
        ParentOrder parent = engine.submit(normalized);

        List<MarketTick> ticks = scenario.getTicks();
        for (MarketTick tick : ticks) {
            engine.onTick(tick);
        }
        engine.onTimeElapsed(normalized.getEndTime());

        ReplayResult result = new ReplayResult();
        result.setFinalProgress(engine.getProgress(parent.getParentOrderId()));
        result.setEvents(new ArrayList<>(publisher.drain()));
        return result;
    }

    public static boolean sameTrace(ReplayResult first, ReplayResult second) {
        List<ExecutionEvent> a = first.getEvents();
        List<ExecutionEvent> b = second.getEvents();
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!sameEvent(a.get(i), b.get(i))) {
                return false;
            }
        }
        return first.getFinalProgress().getCumulativeFilled()
                == second.getFinalProgress().getCumulativeFilled()
                && first.getFinalProgress().getStatus() == second.getFinalProgress().getStatus();
    }

    private static boolean sameEvent(ExecutionEvent a, ExecutionEvent b) {
        return a.getType() == b.getType()
                && a.getQuantity() == b.getQuantity()
                && Double.compare(a.getPrice(), b.getPrice()) == 0
                && a.getTime() == b.getTime()
                && java.util.Objects.equals(a.getDetail(), b.getDetail());
    }

    private ParentOrderRequest normalize(ReplayRequest request) {
        ParentOrderRequest order = request.getOrderRequest();
        order.setRandomSeed(request.getRandomSeed());
        if (order.getStaleQuotePolicy() == null) {
            order.setStaleQuotePolicy(com.github.guoyyit.algoexecutionenginejava.model.StaleQuotePolicy.PAUSE);
        }
        return order;
    }

    private static final class InMemoryOrderStore implements OrderStore {
        private final java.util.Map<String, ParentOrder> parents = new java.util.concurrent.ConcurrentHashMap<>();
        private final List<ExecutionEvent> events = java.util.Collections.synchronizedList(new ArrayList<>());

        @Override
        public void saveParent(ParentOrder parent) {
            parents.put(parent.getParentOrderId(), parent);
        }

        @Override
        public void appendEvent(ExecutionEvent event) {
            events.add(event);
        }

        @Override
        public List<ParentOrder> loadParents() {
            return new ArrayList<>(parents.values());
        }

        @Override
        public List<ExecutionEvent> loadEvents(String parentOrderId) {
            List<ExecutionEvent> result = new ArrayList<>();
            synchronized (events) {
                for (ExecutionEvent event : events) {
                    if (parentOrderId.equals(event.getParentOrderId())) {
                        result.add(event);
                    }
                }
            }
            return result;
        }

        @Override
        public long lastEventSequence() {
            long max = 0L;
            synchronized (events) {
                for (ExecutionEvent event : events) {
                    max = Math.max(max, event.getSequence());
                }
            }
            return max;
        }

        @Override
        public void reset() {
            parents.clear();
            events.clear();
        }
    }
}
