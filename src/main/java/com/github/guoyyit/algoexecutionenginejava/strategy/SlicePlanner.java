package com.github.guoyyit.algoexecutionenginejava.strategy;

import com.github.guoyyit.algoexecutionenginejava.model.ExecutionPlan;
import com.github.guoyyit.algoexecutionenginejava.model.StrategyType;

public interface SlicePlanner {

    StrategyType strategyType();

    ExecutionPlan buildPlan(PlanContext context);
}
