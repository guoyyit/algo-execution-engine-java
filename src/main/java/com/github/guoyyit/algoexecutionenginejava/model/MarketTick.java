package com.github.guoyyit.algoexecutionenginejava.model;

public class MarketTick {
    private String symbol;
    private long time;
    private double bidPrice;
    private double askPrice;
    private double lastPrice;
    private long marketCumVolume;

    public MarketTick() {
    }

    public MarketTick(String symbol, long time, double bidPrice, double askPrice,
                      double lastPrice, long marketCumVolume) {
        this.symbol = symbol;
        this.time = time;
        this.bidPrice = bidPrice;
        this.askPrice = askPrice;
        this.lastPrice = lastPrice;
        this.marketCumVolume = marketCumVolume;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public long getTime() { return time; }
    public void setTime(long time) { this.time = time; }
    public double getBidPrice() { return bidPrice; }
    public void setBidPrice(double bidPrice) { this.bidPrice = bidPrice; }
    public double getAskPrice() { return askPrice; }
    public void setAskPrice(double askPrice) { this.askPrice = askPrice; }
    public double getLastPrice() { return lastPrice; }
    public void setLastPrice(double lastPrice) { this.lastPrice = lastPrice; }
    public long getMarketCumVolume() { return marketCumVolume; }
    public void setMarketCumVolume(long marketCumVolume) { this.marketCumVolume = marketCumVolume; }
}
