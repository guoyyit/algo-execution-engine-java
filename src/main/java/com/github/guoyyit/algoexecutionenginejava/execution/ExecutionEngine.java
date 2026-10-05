package com.github.guoyyit.algoexecutionenginejava.execution;

import com.github.guoyyit.algoexecutionenginejava.market.MatchingEngine;
import com.github.guoyyit.algoexecutionenginejava.market.MarketDataFeed;
import com.github.guoyyit.algoexecutionenginejava.market.SimClock;
import com.github.guoyyit.algoexecutionenginejava.model.EventType;
import com.github.guoyyit.algoexecutionenginejava.model.ExecutionEvent;
import com.github.guoyyit.algoexecutionenginejava.model.MarketTick;
import com.github.guoyyit.algoexecutionenginejava.model.OrderProgress;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrder;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrderRequest;
import com.github.guoyyit.algoexecutionenginejava.model.ParentStatus;
import com.github.guoyyit.algoexecutionenginejava.model.StrategyType;
import com.github.guoyyit.algoexecutionenginejava.service.OrderServiceException;
import com.github.guoyyit.algoexecutionenginejava.strategy.SlicePlanner;
import com.github.guoyyit.algoexecutionenginejava.strategy.TimeWeightedPlanner;
import com.github.guoyyit.algoexecutionenginejava.strategy.VolumeParticipationPlanner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ExecutionEngine {

    private final OrderStore store;
    private final MarketDataFeed feed;
    private final SimClock clock;
    private final Account account;
    private final MatchingEngine matchingEngine;
    private final Map<StrategyType, SlicePlanner> planners;
    private final Map<String, ParentOrderExecutor> executors = new ConcurrentHashMap<>();
    private final IdGenerator idGenerator;
    private final InMemoryEventPublisher publisher;
    private final long defaultStaleQuoteTimeoutMillis;
    private final int defaultSliceCount;
    private final double defaultParticipationRate;

    public ExecutionEngine(OrderStore store, MarketDataFeed feed) {
        this(store, feed, new SimClock(), new Account(Long.MAX_VALUE, Long.MAX_VALUE),
                new MatchingEngine(), new InMemoryEventPublisher(),
                30_000L, 10, 0.1);
    }

    public ExecutionEngine(OrderStore store, MarketDataFeed feed, SimClock clock, Account account,
                           MatchingEngine matchingEngine, InMemoryEventPublisher publisher,
                           long defaultStaleQuoteTimeoutMillis, int defaultSliceCount,
                           double defaultParticipationRate) {
        this.store = store;
        this.feed = feed;
        this.clock = clock;
        this.account = account;
        this.matchingEngine = matchingEngine;
        this.publisher = publisher;
        this.defaultStaleQuoteTimeoutMillis = defaultStaleQuoteTimeoutMillis;
        this.defaultSliceCount = defaultSliceCount;
        this.defaultParticipationRate = defaultParticipationRate;
        this.planners = new HashMap<>();
        TimeWeightedPlanner timePlanner = new TimeWeightedPlanner();
        VolumeParticipationPlanner volumePlanner = new VolumeParticipationPlanner();
        this.planners.put(timePlanner.strategyType(), timePlanner);
        this.planners.put(volumePlanner.strategyType(), volumePlanner);
        long eventStart = store.lastEventSequence();
        this.idGenerator = new IdGenerator(store.loadParents().size(), eventStart);
    }

    public synchronized ParentOrder submit(ParentOrderRequest request) {
        validate(request);
        ParentOrder parent = new ParentOrder();
        parent.setParentOrderId(idGenerator.nextParentId());
        parent.setClientOrderId(request.getClientOrderId());
        parent.setSymbol(request.getSymbol());
        parent.setSide(request.getSide());
        parent.setTotalQuantity(request.getTotalQuantity());
        parent.setStrategy(request.getStrategy());
        parent.setStartTime(request.getStartTime());
        parent.setEndTime(request.getEndTime());
        parent.setSliceCount(request.getSliceCount() > 0 ? request.getSliceCount() : defaultSliceCount);
        parent.setParticipationRate(request.getParticipationRate() > 0
                ? request.getParticipationRate() : defaultParticipationRate);
        parent.setStaleQuotePolicy(request.getStaleQuotePolicy());
        parent.setStaleQuoteTimeoutMillis(request.getStaleQuoteTimeoutMillis() > 0
                ? request.getStaleQuoteTimeoutMillis() : defaultStaleQuoteTimeoutMillis);
        parent.setRandomSeed(request.getRandomSeed());
        parent.setStatus(ParentStatus.CREATED);
        parent.setCreatedAt(clock.millis());
        parent.setLastUpdateTime(clock.millis());
        store.saveParent(parent);
        ParentOrderExecutor executor = newExecutor(parent);
        executors.put(parent.getParentOrderId(), executor);
        executor.initialize();
        return parent;
    }

    public synchronized void pause(String parentOrderId) {
        executor(parentOrderId).pause();
    }

    public synchronized void resume(String parentOrderId) {
        executor(parentOrderId).resume();
    }

    public synchronized void cancel(String parentOrderId) {
        executor(parentOrderId).cancel();
    }

    public OrderProgress getProgress(String parentOrderId) {
        return executor(parentOrderId).progress();
    }

    public ParentOrder getOrder(String parentOrderId) {
        return executor(parentOrderId).getParent();
    }

    public List<OrderProgress> listProgress() {
        List<OrderProgress> result = new ArrayList<>();
        for (ParentOrderExecutor executor : executors.values()) {
            try {
                result.add(executor.progress());
            } catch (RuntimeException ignored) {
            }
        }
        result.sort(Comparator.comparing(OrderProgress::getParentOrderId));
        return result;
    }

    public synchronized void onTick(MarketTick tick) {
        feed.onTick(tick);
        if (tick.getTime() > clock.millis()) {
            clock.setTime(tick.getTime());
        }
        broadcast(executor -> executor.onTick(tick));
    }

    public synchronized void onTimeElapsed(long now) {
        if (now > clock.millis()) {
            clock.setTime(now);
        }
        broadcast(executor -> executor.onTimeElapsed(now));
    }

    public synchronized void recover() {
        List<ParentOrder> parents = store.loadParents();
        long maxParentSeq = 0L;
        for (ParentOrder parent : parents) {
            maxParentSeq = Math.max(maxParentSeq, extractParentSequence(parent.getParentOrderId()));
            ParentOrderExecutor executor = newExecutor(parent);
            executors.put(parent.getParentOrderId(), executor);
            List<ExecutionEvent> events = new ArrayList<>(store.loadEvents(parent.getParentOrderId()));
            events.sort(Comparator.comparingLong(ExecutionEvent::getSequence));
            long maxChildSeq = 0L;
            for (ExecutionEvent event : events) {
                executor.applyRecoveredEvent(event);
                maxChildSeq = Math.max(maxChildSeq, extractChildSequence(event.getChildOrderId()));
            }
            idGenerator.advanceChildSeqTo(parent.getParentOrderId(), maxChildSeq);
            executor.initialize();
        }
        idGenerator.advanceParentSeqTo(maxParentSeq);
        ExecutionEvent recovered = new ExecutionEvent();
        recovered.setSequence(idGenerator.nextEventSequence());
        recovered.setTime(clock.millis());
        recovered.setType(EventType.RECOVERED);
        recovered.setDetail("recovered " + parents.size() + " parent orders from persistent store");
        publisher.publish(recovered);
    }

    public SimClock getClock() {
        return clock;
    }

    public List<ExecutionEvent> events(String parentOrderId) {
        return store.loadEvents(parentOrderId);
    }

    private long extractParentSequence(String parentOrderId) {
        if (parentOrderId == null) {
            return 0L;
        }
        int idx = parentOrderId.indexOf("P-");
        if (idx < 0) {
            return 0L;
        }
        try {
            return Long.parseLong(parentOrderId.substring(idx + 2));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private ParentOrderExecutor newExecutor(ParentOrder parent) {
        return new ParentOrderExecutor(parent, planners, matchingEngine, feed, account, store,
                clock, idGenerator, publisher);
    }

    private void broadcast(Consumer<ParentOrderExecutor> action) {
        for (ParentOrderExecutor executor : executors.values()) {
            try {
                action.accept(executor);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private ParentOrderExecutor executor(String parentOrderId) {
        ParentOrderExecutor executor = executors.get(parentOrderId);
        if (executor == null) {
            throw new OrderServiceException("parent order not found: " + parentOrderId);
        }
        return executor;
    }

    private void validate(ParentOrderRequest request) {
        if (request.getSymbol() == null || request.getSymbol().isBlank()) {
            throw new OrderServiceException("symbol is required");
        }
        if (request.getSide() == null) {
            throw new OrderServiceException("side is required");
        }
        if (request.getStrategy() == null) {
            throw new OrderServiceException("strategy is required");
        }
        if (request.getTotalQuantity() <= 0) {
            throw new OrderServiceException("totalQuantity must be positive");
        }
        if (request.getEndTime() <= request.getStartTime()) {
            throw new OrderServiceException("endTime must be after startTime");
        }
    }

    private long extractChildSequence(String childOrderId) {
        if (childOrderId == null) {
            return 0L;
        }
        int idx = childOrderId.lastIndexOf("-C-");
        if (idx < 0) {
            return 0L;
        }
        try {
            return Long.parseLong(childOrderId.substring(idx + 3));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
