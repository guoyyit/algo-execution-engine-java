package com.github.guoyyit.algoexecutionenginejava.strategy;

import com.github.guoyyit.algoexecutionenginejava.model.ExecutionPlan;
import com.github.guoyyit.algoexecutionenginejava.model.MarketTick;
import com.github.guoyyit.algoexecutionenginejava.model.Slice;
import com.github.guoyyit.algoexecutionenginejava.model.StrategyType;

import java.util.List;

public class VolumeParticipationPlanner implements SlicePlanner {

    private static final long FALLBACK_MARKET_VOLUME_PER_SLICE = 10_000L;

    @Override
    public StrategyType strategyType() {
        return StrategyType.VOLUME_PARTICIPATION;
    }

    @Override
    public ExecutionPlan buildPlan(PlanContext context) {
        long now = context.getNow();
        long windowEnd = context.getWindowEnd();
        int sliceCount = Math.max(1, context.getSliceCount());
        long remaining = context.getRemainingQuantity();
        double participation = context.getParticipationRate() <= 0 ? 0.1 : context.getParticipationRate();

        ExecutionPlan plan = new ExecutionPlan();
        plan.setVersion(context.getPlanVersion());
        plan.setGeneratedAt(now);
        plan.setStrategy(StrategyType.VOLUME_PARTICIPATION);
        plan.setWindowStart(now);
        plan.setWindowEnd(windowEnd);
        plan.setRemainingQuantity(remaining);

        long span = Math.max(1L, windowEnd - now);
        long step = span / sliceCount;
        if (step <= 0) {
            step = 1L;
        }
        long marketVolumePerSlice = forecastMarketVolumePerSlice(context, span, sliceCount);
        long targetPerSlice = Math.max(1L, Math.round(marketVolumePerSlice * participation));

        List<Slice> slices = plan.getSlices();
        long allocated = 0L;
        long cumMarket = context.getLatestTick() == null ? 0L : context.getLatestTick().getMarketCumVolume();
        for (int i = 0; i < sliceCount && allocated < remaining; i++) {
            long plannedTime = now + step * (i + 1L);
            if (i == sliceCount - 1) {
                plannedTime = windowEnd;
            }
            long quantity = Math.min(targetPerSlice, remaining - allocated);
            if (i == sliceCount - 1) {
                quantity = remaining - allocated;
            }
            allocated += quantity;
            cumMarket += marketVolumePerSlice;
            slices.add(new Slice(i, plannedTime, quantity, cumMarket));
        }
        return plan;
    }

    private long forecastMarketVolumePerSlice(PlanContext context, long span, int sliceCount) {
        List<MarketTick> history = context.getHistoryTicks();
        if (history == null || history.size() < 2) {
            return FALLBACK_MARKET_VOLUME_PER_SLICE;
        }
        MarketTick first = history.get(0);
        MarketTick last = history.get(history.size() - 1);
        long volumeSpan = Math.max(1L, last.getMarketCumVolume() - first.getMarketCumVolume());
        long timeSpanMillis = Math.max(1L, last.getTime() - first.getTime());
        double volumePerMillis = (double) volumeSpan / timeSpanMillis;
        double sliceMillis = (double) span / sliceCount;
        long forecast = Math.round(volumePerMillis * sliceMillis);
        return Math.max(1L, forecast);
    }
}
