package com.github.guoyyit.algoexecutionenginejava.strategy;

import com.github.guoyyit.algoexecutionenginejava.model.ExecutionPlan;
import com.github.guoyyit.algoexecutionenginejava.model.PlanSlice;
import com.github.guoyyit.algoexecutionenginejava.model.StrategyType;

import java.util.ArrayList;
import java.util.List;

public class TimeSchedulePlanner implements SlicePlanner {

    @Override
    public StrategyType strategyType() {
        return StrategyType.TIME_SCHEDULE;
    }

    @Override
    public ExecutionPlan buildPlan(PlanContext context) {
        long remainingQty = context.parent().remainingQty();
        int sliceCount = Math.max(1, context.parent().getSliceCount());
        long now = context.now();
        long endTime = context.parent().getEndTime();
        long remainingTime = Math.max(0L, endTime - now);

        List<PlanSlice> slices = new ArrayList<>(sliceCount);
        long allocated = 0L;
        for (int i = 0; i < sliceCount; i++) {
            long scheduledTime;
            if (remainingTime <= 0L) {
                scheduledTime = now;
            } else if (i == sliceCount - 1) {
                scheduledTime = now + remainingTime;
            } else {
                scheduledTime = now + remainingTime * (i + 1) / sliceCount;
            }
            long qty;
            if (i == sliceCount - 1) {
                qty = remainingQty - allocated;
            } else {
                qty = remainingQty / sliceCount;
                allocated += qty;
            }
            slices.add(new PlanSlice(i, scheduledTime, qty));
        }
        return new ExecutionPlan(context.parent().getId(), context.version(), now,
                List.copyOf(slices));
    }
}
