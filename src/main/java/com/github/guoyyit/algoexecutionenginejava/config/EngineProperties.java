package com.github.guoyyit.algoexecutionenginejava.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "algo.engine")
public class EngineProperties {

    private String eventLogDir = "data/eventlog";
    private long tickIntervalMillis = 100L;
    private int defaultSliceCount = 6;
    private double defaultParticipationRate = 0.1d;
    private long staleQuoteMillis = 500L;
    private String staleQuoteAction = "PAUSE";
    private long orderTimeoutMillis = 2000L;
    private long accountMaxPosition = 1_000_000L;
    private long accountMaxCash = 1_000_000_000L;

    public String getEventLogDir() {
        return eventLogDir;
    }

    public void setEventLogDir(String eventLogDir) {
        this.eventLogDir = eventLogDir;
    }

    public long getTickIntervalMillis() {
        return tickIntervalMillis;
    }

    public void setTickIntervalMillis(long tickIntervalMillis) {
        this.tickIntervalMillis = tickIntervalMillis;
    }

    public int getDefaultSliceCount() {
        return defaultSliceCount;
    }

    public void setDefaultSliceCount(int defaultSliceCount) {
        this.defaultSliceCount = defaultSliceCount;
    }

    public double getDefaultParticipationRate() {
        return defaultParticipationRate;
    }

    public void setDefaultParticipationRate(double defaultParticipationRate) {
        this.defaultParticipationRate = defaultParticipationRate;
    }

    public long getStaleQuoteMillis() {
        return staleQuoteMillis;
    }

    public void setStaleQuoteMillis(long staleQuoteMillis) {
        this.staleQuoteMillis = staleQuoteMillis;
    }

    public String getStaleQuoteAction() {
        return staleQuoteAction;
    }

    public void setStaleQuoteAction(String staleQuoteAction) {
        this.staleQuoteAction = staleQuoteAction;
    }

    public long getOrderTimeoutMillis() {
        return orderTimeoutMillis;
    }

    public void setOrderTimeoutMillis(long orderTimeoutMillis) {
        this.orderTimeoutMillis = orderTimeoutMillis;
    }

    public long getAccountMaxPosition() {
        return accountMaxPosition;
    }

    public void setAccountMaxPosition(long accountMaxPosition) {
        this.accountMaxPosition = accountMaxPosition;
    }

    public long getAccountMaxCash() {
        return accountMaxCash;
    }

    public void setAccountMaxCash(long accountMaxCash) {
        this.accountMaxCash = accountMaxCash;
    }
}
