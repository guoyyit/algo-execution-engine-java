package com.github.guoyyit.algoexecutionenginejava.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.guoyyit.algoexecutionenginejava.config.EngineProperties;
import com.github.guoyyit.algoexecutionenginejava.engine.ExecutionEngine;
import com.github.guoyyit.algoexecutionenginejava.persist.EventSourcedOrderStore;
import com.github.guoyyit.algoexecutionenginejava.persist.FileEventLog;
import com.github.guoyyit.algoexecutionenginejava.simulation.MarketDataSimulator;
import com.github.guoyyit.algoexecutionenginejava.simulation.MatchingEngine;
import com.github.guoyyit.algoexecutionenginejava.simulation.TickClock;
import com.github.guoyyit.algoexecutionenginejava.simulation.TradingAccount;
import com.github.guoyyit.algoexecutionenginejava.strategy.SlicePlanner;
import com.github.guoyyit.algoexecutionenginejava.strategy.TimeSchedulePlanner;
import com.github.guoyyit.algoexecutionenginejava.strategy.VolumeParticipationPlanner;

import java.nio.file.Path;
import java.util.List;
import java.util.Random;

final class EngineTestSupport {

    private EngineTestSupport() {
    }

    static EngineProperties properties(Path logDir) {
        EngineProperties properties = new EngineProperties();
        properties.setEventLogDir(logDir.toString());
        properties.setTickIntervalMillis(100L);
        properties.setDefaultSliceCount(4);
        properties.setDefaultParticipationRate(0.2d);
        properties.setStaleQuoteMillis(300L);
        properties.setStaleQuoteAction("PAUSE");
        properties.setOrderTimeoutMillis(1_000_000L);
        return properties;
    }

    static ExecutionEngine newEngine(Path logDir, EngineProperties properties) {
        TickClock clock = new TickClock(properties.getTickIntervalMillis());
        MarketDataSimulator marketData = new MarketDataSimulator(clock);
        MatchingEngine matchingEngine = new MatchingEngine(marketData, clock, new Random(42L));
        TradingAccount account = new TradingAccount(properties.getAccountMaxPosition(),
                properties.getAccountMaxCash());
        ObjectMapper objectMapper = new ObjectMapper();
        FileEventLog eventLog = new FileEventLog(logDir, objectMapper);
        EventSourcedOrderStore store = new EventSourcedOrderStore(eventLog, objectMapper);
        List<SlicePlanner> planners = List.of(new TimeSchedulePlanner(), new VolumeParticipationPlanner());
        ExecutionEngine engine = new ExecutionEngine(clock, marketData, matchingEngine, account,
                store, properties, planners, objectMapper);
        engine.start();
        return engine;
    }
}
