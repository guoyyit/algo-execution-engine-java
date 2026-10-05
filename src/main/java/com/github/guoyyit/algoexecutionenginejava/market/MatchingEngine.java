package com.github.guoyyit.algoexecutionenginejava.market;

import com.github.guoyyit.algoexecutionenginejava.model.ChildOrder;
import com.github.guoyyit.algoexecutionenginejava.model.MarketTick;

import java.util.Random;

public class MatchingEngine {

    private final double minFillRatio;
    private final double maxFillRatio;

    public MatchingEngine() {
        this(0.3, 1.0);
    }

    public MatchingEngine(double minFillRatio, double maxFillRatio) {
        this.minFillRatio = minFillRatio;
        this.maxFillRatio = maxFillRatio;
    }

    public FillResult simulateFill(ChildOrder child, MarketTick tick, Random random) {
        long outstanding = child.getOutstandingQuantity();
        if (outstanding <= 0 || tick == null) {
            return new FillResult(0L, 0.0);
        }
        double ratio = minFillRatio
                + random.nextDouble() * (maxFillRatio - minFillRatio);
        long filled = Math.min(outstanding, Math.max(1L, Math.round(outstanding * ratio)));
        if (random.nextDouble() < 0.12) {
            filled = Math.min(outstanding, Math.max(1L, filled / 3));
        }
        double price = child.getSide() == com.github.guoyyit.algoexecutionenginejava.model.Side.BUY
                ? tick.getAskPrice()
                : tick.getBidPrice();
        double slip = (random.nextDouble() - 0.5) * 0.01 * price;
        return new FillResult(filled, Math.round((price + slip) * 100.0) / 100.0);
    }
}
