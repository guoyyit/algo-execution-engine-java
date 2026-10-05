package com.github.guoyyit.algoexecutionenginejava.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "algo.engine")
public class EngineProperties {

    private String storeDir = "data/engine";
    private long accountCashLimit = 1_000_000L;
    private long accountPositionLimit = 1_000_000L;
    private long defaultStaleQuoteTimeoutMillis = 30_000L;
    private int defaultSliceCount = 10;
    private double defaultParticipationRate = 0.1;

    public String getStoreDir() { return storeDir; }
    public void setStoreDir(String storeDir) { this.storeDir = storeDir; }
    public long getAccountCashLimit() { return accountCashLimit; }
    public void setAccountCashLimit(long accountCashLimit) { this.accountCashLimit = accountCashLimit; }
    public long getAccountPositionLimit() { return accountPositionLimit; }
    public void setAccountPositionLimit(long accountPositionLimit) { this.accountPositionLimit = accountPositionLimit; }
    public long getDefaultStaleQuoteTimeoutMillis() { return defaultStaleQuoteTimeoutMillis; }
    public void setDefaultStaleQuoteTimeoutMillis(long defaultStaleQuoteTimeoutMillis) { this.defaultStaleQuoteTimeoutMillis = defaultStaleQuoteTimeoutMillis; }
    public int getDefaultSliceCount() { return defaultSliceCount; }
    public void setDefaultSliceCount(int defaultSliceCount) { this.defaultSliceCount = defaultSliceCount; }
    public double getDefaultParticipationRate() { return defaultParticipationRate; }
    public void setDefaultParticipationRate(double defaultParticipationRate) { this.defaultParticipationRate = defaultParticipationRate; }
}
