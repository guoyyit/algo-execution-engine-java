package com.github.guoyyit.algoexecutionenginejava.config;

import com.github.guoyyit.algoexecutionenginejava.execution.Account;
import com.github.guoyyit.algoexecutionenginejava.execution.ExecutionEngine;
import com.github.guoyyit.algoexecutionenginejava.execution.FileOrderStore;
import com.github.guoyyit.algoexecutionenginejava.execution.InMemoryEventPublisher;
import com.github.guoyyit.algoexecutionenginejava.execution.OrderStore;
import com.github.guoyyit.algoexecutionenginejava.market.MarketDataFeed;
import com.github.guoyyit.algoexecutionenginejava.market.MatchingEngine;
import com.github.guoyyit.algoexecutionenginejava.market.ReplayMarketDataFeed;
import com.github.guoyyit.algoexecutionenginejava.market.SimClock;
import com.github.guoyyit.algoexecutionenginejava.replay.ReplayRunner;
import com.github.guoyyit.algoexecutionenginejava.service.AlgoExecutionService;
import com.github.guoyyit.algoexecutionenginejava.service.DefaultAlgoExecutionService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
@EnableConfigurationProperties(EngineProperties.class)
public class EngineConfig {

    @Bean
    public OrderStore orderStore(EngineProperties properties) {
        return new FileOrderStore(Path.of(properties.getStoreDir()));
    }

    @Bean
    public MarketDataFeed marketDataFeed() {
        return new ReplayMarketDataFeed();
    }

    @Bean
    public SimClock simClock() {
        return new SimClock();
    }

    @Bean
    public MatchingEngine matchingEngine() {
        return new MatchingEngine();
    }

    @Bean
    public InMemoryEventPublisher eventPublisher() {
        return new InMemoryEventPublisher();
    }

    @Bean
    public Account account(EngineProperties properties) {
        return new Account(properties.getAccountCashLimit(), properties.getAccountPositionLimit());
    }

    @Bean
    public ExecutionEngine executionEngine(OrderStore store, MarketDataFeed feed, SimClock clock,
                                           Account account, MatchingEngine matchingEngine,
                                           InMemoryEventPublisher publisher, EngineProperties properties) {
        ExecutionEngine engine = new ExecutionEngine(store, feed, clock, account, matchingEngine,
                publisher, properties.getDefaultStaleQuoteTimeoutMillis(),
                properties.getDefaultSliceCount(), properties.getDefaultParticipationRate());
        engine.recover();
        return engine;
    }

    @Bean
    public AlgoExecutionService algoExecutionService(ExecutionEngine engine) {
        return new DefaultAlgoExecutionService(engine);
    }

    @Bean
    public ReplayRunner replayRunner() {
        return new ReplayRunner();
    }
}
