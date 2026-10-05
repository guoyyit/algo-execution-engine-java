package com.github.guoyyit.algoexecutionenginejava.strategy;

import com.github.guoyyit.algoexecutionenginejava.model.ExecutionPlan;
import com.github.guoyyit.algoexecutionenginejava.model.PlanSlice;
import com.github.guoyyit.algoexecutionenginejava.model.StrategyType;

import java.util.ArrayList;
import java.util.List;

public class VolumeParticipationPlanner implements SlicePlanner {

    @Override
    public StrategyType strategyType() {
        return StrategyType.VOLUME_PARTICIPATION;
    }

    @Override
    public ExecutionPlan buildPlan(PlanContext context) {
        long remainingQty = context.parent().remainingQty();
        int sliceCount = Math.max(1, context.parent().getSliceCount());

        List<PlanSlice> slices = new ArrayList<>(sliceCount);
        long allocated = 0L;
        for (int i = 0; i < sliceCount; i++) {
            long qty;
            if (i == sliceCount - 1) {
                qty = remainingQty - allocated;
            } else {
                qty = remainingQty / sliceCount;
                allocated += qty;
            }
            slices.add(new PlanSlice(i, context.now(), qty));
        }
        return new ExecutionPlan(context.parent().getId(), context.version(), context.now(),
                List.copyOf(slices));
    }

    public long targetCumQty(long marketVolumeAtPlanStart, long currentMarketVolume,
                             double participationRate, long maxTarget) {
        long tradedInMarket = Math.max(0L, currentMarketVolume - marketVolumeAtPlanStart);
        return Math.min(maxTarget, (long) (tradedInMarket * participationRate));
    }
}
