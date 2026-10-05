package com.github.guoyyit.algoexecutionenginejava.strategy;

import com.github.guoyyit.algoexecutionenginejava.model.ParentOrder;
import com.github.guoyyit.algoexecutionenginejava.model.Quote;

public record PlanContext(
        ParentOrder parent,
        long now,
        int version,
        Quote currentQuote
) {
}
