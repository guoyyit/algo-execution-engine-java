package com.github.guoyyit.algoexecutionenginejava.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.guoyyit.algoexecutionenginejava.config.EngineProperties;
import com.github.guoyyit.algoexecutionenginejava.model.ChildOrder;
import com.github.guoyyit.algoexecutionenginejava.model.Fill;
import com.github.guoyyit.algoexecutionenginejava.model.OrderRequest;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrder;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrderStatus;
import com.github.guoyyit.algoexecutionenginejava.model.StrategyType;
import com.github.guoyyit.algoexecutionenginejava.persist.OrderStore;
import com.github.guoyyit.algoexecutionenginejava.persist.FileEventLog;
import com.github.guoyyit.algoexecutionenginejava.simulation.MarketDataSimulator;
import com.github.guoyyit.algoexecutionenginejava.simulation.MatchingEngine;
import com.github.guoyyit.algoexecutionenginejava.simulation.SimClock;
import com.github.guoyyit.algoexecutionenginejava.simulation.TickClock;
import com.github.guoyyit.algoexecutionenginejava.simulation.TradingAccount;
import com.github.guoyyit.algoexecutionenginejava.strategy.SlicePlanner;
import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ExecutionEngine {

    private final SimClock clock;
    private final MarketDataSimulator marketData;
    private final MatchingEngine matchingEngine;
    private final TradingAccount account;
    private final OrderStore store;
    private final EngineProperties properties;
    private final ObjectMapper objectMapper;
    private final Path marketStateFile;
    private final Map<StrategyType, SlicePlanner> planners = new HashMap<>();
    private final ConcurrentHashMap<String, ParentExecutor> executors = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(0);
    private volatile boolean running;

    public ExecutionEngine() {
        this.clock = null;
        this.marketData = null;
        this.matchingEngine = null;
        this.account = null;
        this.store = null;
        this.properties = null;
        this.objectMapper = null;
        this.marketStateFile = null;
    }

    public ExecutionEngine(TickClock clock, MarketDataSimulator marketData, MatchingEngine matchingEngine,
                           TradingAccount account, OrderStore store, EngineProperties properties,
                           List<SlicePlanner> plannerList, ObjectMapper objectMapper) {
        this.clock = clock;
        this.marketData = marketData;
        this.matchingEngine = matchingEngine;
        this.account = account;
        this.store = store;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.marketStateFile = Path.of(properties.getEventLogDir()).resolve("market-state.json");
        for (SlicePlanner planner : plannerList) {
            planners.put(planner.strategyType(), planner);
        }
        long maxId = store.allParents().stream()
                .mapToLong(parent -> parseId(parent.getId()))
                .max().orElse(0L);
        idSequence.set(maxId);
    }

    @PostConstruct
    public void start() {
        if (store == null) {
            return;
        }
        if (eventLog() instanceof FileEventLog fileEventLog) {
            clock.restoreTick(fileEventLog.readTickWatermark());
        }
        marketData.restoreFrom(marketStateFile, objectMapper);
        running = true;
        for (ParentOrder parent : store.allParents()) {
            recover(parent);
        }
    }

    public void stop() {
        running = false;
    }

    public ParentOrder submit(OrderRequest request) {
        requireRunning();
        validate(request);
        long id = idSequence.incrementAndGet();
        ParentOrder parent = new ParentOrder();
        parent.setId(String.format("P%08d", id));
        parent.setSymbol(request.symbol());
        parent.setSide(request.side());
        parent.setTotalQty(request.totalQty());
        parent.setFilledQty(0L);
        parent.setSubmittedQty(0L);
        parent.setStatus(ParentOrderStatus.NEW);
        parent.setStartTime(clock.now());
        parent.setEndTime(clock.now() + Math.max(0L, request.durationMillis()));
        parent.setStrategy(request.strategy());
        parent.setSliceCount(request.sliceCount() != null && request.sliceCount() > 0
                ? request.sliceCount() : properties.getDefaultSliceCount());
        parent.setParticipationRate(request.participationRate() != null
                ? request.participationRate() : properties.getDefaultParticipationRate());
        parent.setSeed(request.seed() != null ? request.seed() : id);
        parent.setPlanVersion(0);
        parent.setCreatedAt(clock.now());
        store.saveParent(parent);

        marketData.registerSymbol(parent.getSymbol(), 100L, 1000L,
                parent.getSeed() ^ 0x9E3779B97F4A7C15L);

        ParentExecutor executor = new ParentExecutor(this, parent);
        executors.put(parent.getId(), executor);
        executor.activate();
        return store.getParent(parent.getId()).orElse(parent);
    }

    public ParentOrder pause(String parentOrderId) {
        requireRunning();
        ParentExecutor executor = executors.get(parentOrderId);
        if (executor != null) {
            executor.pause();
        }
        return getParent(parentOrderId);
    }

    public ParentOrder resume(String parentOrderId) {
        requireRunning();
        ParentExecutor executor = executors.get(parentOrderId);
        if (executor != null) {
            executor.resume();
        }
        return getParent(parentOrderId);
    }

    public ParentOrder cancel(String parentOrderId) {
        requireRunning();
        ParentExecutor executor = executors.get(parentOrderId);
        if (executor != null) {
            executor.cancel();
        }
        return getParent(parentOrderId);
    }

    public ParentOrder getParent(String parentOrderId) {
        return store.getParent(parentOrderId).orElse(null);
    }

    public List<ParentOrder> allParents() {
        return store.allParents();
    }

    public void runTicks(int ticks) {
        requireRunning();
        for (int i = 0; i < ticks; i++) {
            stepOnce();
        }
    }

    public void stepOnce() {
        long now = clock.tick();
        if (eventLog() instanceof FileEventLog fileEventLog) {
            fileEventLog.writeTickWatermark(clock.currentTick());
        }
        for (String symbol : activeSymbols()) {
            var quote = marketData.advance(symbol);
            for (Fill fill : matchingEngine.onTick(quote)) {
                ChildOrder child = store.getChild(fill.childOrderId()).orElseThrow();
                account.onFill(child.getParentOrderId(), quote.symbol(),
                        store.getParent(child.getParentOrderId()).orElseThrow().getSide(),
                        fill.qty(), fill.price());
                store.saveChild(child);
            }
        }
        marketData.saveTo(marketStateFile, objectMapper);
        for (ParentExecutor executor : new ArrayList<>(executors.values())) {
            try {
                executor.onTick(now);
            } catch (RuntimeException e) {
                executor.markFailed(e);
            }
        }
    }

    public SimClock clock() {
        return clock;
    }

    public MarketDataSimulator marketData() {
        return marketData;
    }

    public MatchingEngine matchingEngine() {
        return matchingEngine;
    }

    public TradingAccount account() {
        return account;
    }

    public OrderStore store() {
        return store;
    }

    public com.github.guoyyit.algoexecutionenginejava.persist.EventLog eventLog() {
        if (store instanceof com.github.guoyyit.algoexecutionenginejava.persist.EventSourcedOrderStore sourced) {
            return sourced.eventLog();
        }
        return null;
    }

    public EngineProperties properties() {
        return properties;
    }

    public SlicePlanner planner(StrategyType type) {
        return planners.get(type);
    }

    public boolean isRunning() {
        return running;
    }

    void removeExecutor(String parentOrderId) {
        executors.remove(parentOrderId);
    }

    private Set<String> activeSymbols() {
        Set<String> symbols = new HashSet<>();
        for (ParentOrder parent : store.allParents()) {
            symbols.add(parent.getSymbol());
        }
        return symbols;
    }

    private void recover(ParentOrder parent) {
        for (ChildOrder child : store.childrenOf(parent.getId())) {
            if (child.getStatus().isActive()) {
                matchingEngine.restoreResting(child, parent.getSymbol());
            }
        }
        if (!parent.getStatus().isTerminal()) {
            ParentExecutor executor = new ParentExecutor(this, parent);
            executors.put(parent.getId(), executor);
            executor.recoverState();
        }
    }

    private void validate(OrderRequest request) {
        if (request == null || request.symbol() == null || request.symbol().isBlank()) {
            throw new IllegalArgumentException("");
        }
        if (request.side() == null) {
            throw new IllegalArgumentException("");
        }
        if (request.strategy() == null) {
            throw new IllegalArgumentException("");
        }
        if (request.totalQty() <= 0) {
            throw new IllegalArgumentException("");
        }
        if (request.durationMillis() <= 0) {
            throw new IllegalArgumentException("");
        }
    }

    private void requireRunning() {
        if (!running) {
            throw new IllegalStateException("execution engine is not running");
        }
    }

    private long parseId(String id) {
        try {
            return Long.parseLong(id.replace("P", ""));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
