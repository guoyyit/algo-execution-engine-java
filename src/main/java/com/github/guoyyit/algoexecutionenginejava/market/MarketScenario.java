package com.github.guoyyit.algoexecutionenginejava.market;

import com.github.guoyyit.algoexecutionenginejava.model.MarketTick;

import java.util.ArrayList;
import java.util.List;

public class MarketScenario {
    private String symbol;
    private List<MarketTick> ticks = new ArrayList<>();

    public MarketScenario() {
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public List<MarketTick> getTicks() { return ticks; }
    public void setTicks(List<MarketTick> ticks) { this.ticks = ticks; }
}
