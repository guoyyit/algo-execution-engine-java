package com.github.guoyyit.algoexecutionenginejava.model;

public class ParentOrderRequest {
    private String clientOrderId;
    private String symbol;
    private Side side;
    private long totalQuantity;
    private StrategyType strategy;
    private long startTime;
    private long endTime;
    private int sliceCount;
    private double participationRate;
    private StaleQuotePolicy staleQuotePolicy;
    private long staleQuoteTimeoutMillis;
    private long randomSeed;

    public String getClientOrderId() { return clientOrderId; }
    public void setClientOrderId(String clientOrderId) { this.clientOrderId = clientOrderId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public Side getSide() { return side; }
    public void setSide(Side side) { this.side = side; }
    public long getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(long totalQuantity) { this.totalQuantity = totalQuantity; }
    public StrategyType getStrategy() { return strategy; }
    public void setStrategy(StrategyType strategy) { this.strategy = strategy; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public int getSliceCount() { return sliceCount; }
    public void setSliceCount(int sliceCount) { this.sliceCount = sliceCount; }
    public double getParticipationRate() { return participationRate; }
    public void setParticipationRate(double participationRate) { this.participationRate = participationRate; }
    public StaleQuotePolicy getStaleQuotePolicy() { return staleQuotePolicy; }
    public void setStaleQuotePolicy(StaleQuotePolicy staleQuotePolicy) { this.staleQuotePolicy = staleQuotePolicy; }
    public long getStaleQuoteTimeoutMillis() { return staleQuoteTimeoutMillis; }
    public void setStaleQuoteTimeoutMillis(long staleQuoteTimeoutMillis) { this.staleQuoteTimeoutMillis = staleQuoteTimeoutMillis; }
    public long getRandomSeed() { return randomSeed; }
    public void setRandomSeed(long randomSeed) { this.randomSeed = randomSeed; }
}
