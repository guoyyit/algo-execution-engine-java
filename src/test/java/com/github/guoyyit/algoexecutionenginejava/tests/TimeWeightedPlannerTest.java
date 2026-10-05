package com.github.guoyyit.algoexecutionenginejava.tests;

import com.github.guoyyit.algoexecutionenginejava.model.ExecutionPlan;
import com.github.guoyyit.algoexecutionenginejava.model.Slice;
import com.github.guoyyit.algoexecutionenginejava.model.StrategyType;
import com.github.guoyyit.algoexecutionenginejava.strategy.PlanContext;
import com.github.guoyyit.algoexecutionenginejava.strategy.TimeWeightedPlanner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeWeightedPlannerTest {

    private final TimeWeightedPlanner planner = new TimeWeightedPlanner();

    @Test
    void splitsEvenlyAndKeepsRemainderInLastSlice() {
        PlanContext context = context(10_000L, 10_000L, 60_000L, 8, 1);
        ExecutionPlan plan = planner.buildPlan(context);

        assertEquals(8, plan.getSlices().size());
        assertEquals(StrategyType.TIME_WEIGHTED, plan.getStrategy());
        assertEquals(10_000L, plan.totalPlannedQuantity());
        long sum = 0L;
        for (Slice slice : plan.getSlices()) {
            sum += slice.getPlannedQuantity();
            assertTrue(slice.getPlannedTime() > 10_000L);
            assertTrue(slice.getPlannedTime() <= 60_000L);
        }
        assertEquals(10_000L, sum);
        Slice last = plan.getSlices().get(7);
        assertEquals(60_000L, last.getPlannedTime());
    }

    @Test
    void regeneratedPlanCoversOnlyRemainingQuantity() {
        PlanContext first = context(10_000L, 0L, 100_000L, 10, 1);
        ExecutionPlan firstPlan = planner.buildPlan(first);
        assertEquals(10_000L, firstPlan.totalPlannedQuantity());

        PlanContext resumed = context(3_333L, 40_000L, 100_000L, 6, 2);
        ExecutionPlan resumedPlan = planner.buildPlan(resumed);
        assertEquals(2, resumedPlan.getVersion());
        assertEquals(3_333L, resumedPlan.totalPlannedQuantity());
        assertEquals(3_333L, resumedPlan.getRemainingQuantity());
    }

    @Test
    void planIsDeterministicForSameInputs() {
        ExecutionPlan a = planner.buildPlan(context(7_777L, 1_000L, 99_000L, 7, 1));
        ExecutionPlan b = planner.buildPlan(context(7_777L, 1_000L, 99_000L, 7, 1));
        for (int i = 0; i < a.getSlices().size(); i++) {
            assertEquals(a.getSlices().get(i).getPlannedQuantity(), b.getSlices().get(i).getPlannedQuantity());
            assertEquals(a.getSlices().get(i).getPlannedTime(), b.getSlices().get(i).getPlannedTime());
        }
    }

    private PlanContext context(long remaining, long start, long end, int slices, int version) {
        PlanContext context = new PlanContext();
        context.setStrategy(StrategyType.TIME_WEIGHTED);
        context.setNow(start);
        context.setWindowStart(start);
        context.setWindowEnd(end);
        context.setSliceCount(slices);
        context.setRemainingQuantity(remaining);
        context.setPlanVersion(version);
        context.setParticipationRate(0.1);
        return context;
    }
}
