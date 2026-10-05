package com.github.guoyyit.algoexecutionenginejava.market;

import com.github.guoyyit.algoexecutionenginejava.model.MarketTick;

import java.util.List;

public interface MarketDataFeed {

    MarketTick latest(String symbol);

    List<MarketTick> history(String symbol);

    void onTick(MarketTick tick);
}
