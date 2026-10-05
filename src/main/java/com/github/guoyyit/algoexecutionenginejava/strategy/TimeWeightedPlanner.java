package com.github.guoyyit.algoexecutionenginejava.strategy;

import com.github.guoyyit.algoexecutionenginejava.model.ExecutionPlan;
import com.github.guoyyit.algoexecutionenginejava.model.Slice;
import com.github.guoyyit.algoexecutionenginejava.model.StrategyType;

import java.util.List;

public class TimeWeightedPlanner implements SlicePlanner {

    @Override
    public StrategyType strategyType() {
        return StrategyType.TIME_WEIGHTED;
    }

    @Override
    public ExecutionPlan buildPlan(PlanContext context) {
        long now = context.getNow();
        long windowEnd = context.getWindowEnd();
        int sliceCount = Math.max(1, context.getSliceCount());
        long remaining = context.getRemainingQuantity();

        ExecutionPlan plan = new ExecutionPlan();
        plan.setVersion(context.getPlanVersion());
        plan.setGeneratedAt(now);
        plan.setStrategy(StrategyType.TIME_WEIGHTED);
        plan.setWindowStart(now);
        plan.setWindowEnd(windowEnd);
        plan.setRemainingQuantity(remaining);

        long span = Math.max(1L, windowEnd - now);
        long step = span / sliceCount;
        if (step <= 0) {
            step = 1L;
        }
        List<Slice> slices = plan.getSlices();
        long allocated = 0L;
        long base = remaining / sliceCount;
        long carry = remaining % sliceCount;
        long cumVolume = context.getLatestTick() == null ? 0L : context.getLatestTick().getMarketCumVolume();
        for (int i = 0; i < sliceCount && allocated < remaining; i++) {
            long plannedTime = now + step * (i + 1L);
            if (i == sliceCount - 1) {
                plannedTime = windowEnd;
            }
            long quantity = base + (i < carry ? 1L : 0L);
            if (i == sliceCount - 1) {
                quantity = remaining - allocated;
            }
            allocated += quantity;
            slices.add(new Slice(i, plannedTime, quantity, cumVolume));
        }
        return plan;
    }
}
