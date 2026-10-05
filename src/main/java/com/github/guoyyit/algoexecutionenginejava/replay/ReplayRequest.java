package com.github.guoyyit.algoexecutionenginejava.replay;

import com.github.guoyyit.algoexecutionenginejava.market.MarketScenario;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrderRequest;

public class ReplayRequest {
    private long randomSeed;
    private ParentOrderRequest orderRequest;
    private MarketScenario scenario;

    public long getRandomSeed() { return randomSeed; }
    public void setRandomSeed(long randomSeed) { this.randomSeed = randomSeed; }
    public ParentOrderRequest getOrderRequest() { return orderRequest; }
    public void setOrderRequest(ParentOrderRequest orderRequest) { this.orderRequest = orderRequest; }
    public MarketScenario getScenario() { return scenario; }
    public void setScenario(MarketScenario scenario) { this.scenario = scenario; }
}
