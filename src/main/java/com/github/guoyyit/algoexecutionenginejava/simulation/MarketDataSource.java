package com.github.guoyyit.algoexecutionenginejava.simulation;

import com.github.guoyyit.algoexecutionenginejava.model.Quote;

public interface MarketDataSource {

    Quote latestQuote(String symbol);

    Quote advance(String symbol);
}
