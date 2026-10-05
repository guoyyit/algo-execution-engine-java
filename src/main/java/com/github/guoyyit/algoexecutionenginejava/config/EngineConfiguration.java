package com.github.guoyyit.algoexecutionenginejava.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.guoyyit.algoexecutionenginejava.engine.ExecutionEngine;
import com.github.guoyyit.algoexecutionenginejava.persist.EventLog;
import com.github.guoyyit.algoexecutionenginejava.persist.EventSourcedOrderStore;
import com.github.guoyyit.algoexecutionenginejava.persist.FileEventLog;
import com.github.guoyyit.algoexecutionenginejava.persist.OrderStore;
import com.github.guoyyit.algoexecutionenginejava.service.AlgoExecutionService;
import com.github.guoyyit.algoexecutionenginejava.simulation.MarketDataSimulator;
import com.github.guoyyit.algoexecutionenginejava.simulation.MatchingEngine;
import com.github.guoyyit.algoexecutionenginejava.simulation.TickClock;
import com.github.guoyyit.algoexecutionenginejava.simulation.TradingAccount;
import com.github.guoyyit.algoexecutionenginejava.strategy.SlicePlanner;
import com.github.guoyyit.algoexecutionenginejava.strategy.TimeSchedulePlanner;
import com.github.guoyyit.algoexecutionenginejava.strategy.VolumeParticipationPlanner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.List;

@Configuration
@EnableConfigurationProperties(EngineProperties.class)
public class EngineConfiguration {

    @Bean
    public TickClock tickClock(EngineProperties properties) {
        return new TickClock(properties.getTickIntervalMillis());
    }

    @Bean
    public MarketDataSimulator marketDataSimulator(TickClock clock) {
        return new MarketDataSimulator(clock);
    }

    @Bean
    public MatchingEngine matchingEngine(MarketDataSimulator marketData, TickClock clock) {
        return new MatchingEngine(marketData, clock, null);
    }

    @Bean
    public TradingAccount tradingAccount(EngineProperties properties) {
        return new TradingAccount(properties.getAccountMaxPosition(), properties.getAccountMaxCash());
    }

    @Bean
    public EventLog eventLog(EngineProperties properties, ObjectMapper objectMapper) {
        return new FileEventLog(Path.of(properties.getEventLogDir()), objectMapper);
    }

    @Bean
    public OrderStore orderStore(EventLog eventLog, ObjectMapper objectMapper) {
        return new EventSourcedOrderStore(eventLog, objectMapper);
    }

    @Bean
    public List<SlicePlanner> slicePlanners() {
        return List.of(new TimeSchedulePlanner(), new VolumeParticipationPlanner());
    }

    @Bean
    public ExecutionEngine executionEngine(TickClock clock, MarketDataSimulator marketData,
                                           MatchingEngine matchingEngine, TradingAccount account,
                                           OrderStore orderStore, EngineProperties properties,
                                           List<SlicePlanner> slicePlanners, ObjectMapper objectMapper) {
        return new ExecutionEngine(clock, marketData, matchingEngine, account, orderStore,
                properties, slicePlanners, objectMapper);
    }

    @Bean
    public AlgoExecutionService algoExecutionService(ExecutionEngine executionEngine) {
        return new AlgoExecutionService(executionEngine);
    }
}
