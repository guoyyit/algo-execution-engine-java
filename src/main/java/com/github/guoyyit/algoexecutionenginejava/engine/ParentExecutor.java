package com.github.guoyyit.algoexecutionenginejava.engine;

import com.github.guoyyit.algoexecutionenginejava.model.ChildOrder;
import com.github.guoyyit.algoexecutionenginejava.model.ChildOrderStatus;
import com.github.guoyyit.algoexecutionenginejava.model.DeviationRecord;
import com.github.guoyyit.algoexecutionenginejava.model.ExecutionPlan;
import com.github.guoyyit.algoexecutionenginejava.model.Fill;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrder;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrderStatus;
import com.github.guoyyit.algoexecutionenginejava.model.PlanSlice;
import com.github.guoyyit.algoexecutionenginejava.model.Quote;
import com.github.guoyyit.algoexecutionenginejava.model.StrategyType;
import com.github.guoyyit.algoexecutionenginejava.strategy.PlanContext;
import com.github.guoyyit.algoexecutionenginejava.strategy.VolumeParticipationPlanner;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ParentExecutor {

    private final ExecutionEngine engine;
    private final ParentOrder parent;
    private final AtomicLong childIdSequence = new AtomicLong(0);

    private ExecutionPlan plan;
    private int nextSliceIndex;
    private long volumeBaseline = -1L;
    private long staleSince = -1L;
    private long lastDeviationTick = -1L;
    private boolean degraded;
    private boolean finished;
    private String lastError;

    public ParentExecutor(ExecutionEngine engine, ParentOrder parentOrder) {
        this.engine = engine;
        this.parent = parentOrder;
        long maxChildId = engine.store().childrenOf(parent.getId()).stream()
                .mapToLong(child -> parseChildId(child.getId()))
                .max().orElse(0L);
        childIdSequence.set(maxChildId);
    }

    public void activate() {
        transitionTo(ParentOrderStatus.ACTIVE);
        regeneratePlan();
    }

    public void recoverState() {
        plan = engine.store().latestPlan(parent.getId()).orElse(null);
        if (plan != null) {
            nextSliceIndex = (int) engine.store().childrenOf(parent.getId()).stream()
                    .filter(child -> child.getPlanVersion() == plan.getVersion())
                    .count();
            nextSliceIndex = Math.min(nextSliceIndex, plan.getSlices().size());
        }
        if (parent.getStatus() == ParentOrderStatus.NEW) {
            transitionTo(ParentOrderStatus.ACTIVE);
            if (plan == null) {
                regeneratePlan();
            }
        }
    }

    public synchronized void pause() {
        if (parent.getStatus().isTerminal() || parent.getStatus() == ParentOrderStatus.PAUSED
                || parent.getStatus() == ParentOrderStatus.CANCELLING) {
            return;
        }
        cancelAllActiveChildren();
        transitionTo(ParentOrderStatus.PAUSED);
    }

    public synchronized void resume() {
        if (parent.getStatus() != ParentOrderStatus.PAUSED) {
            return;
        }
        transitionTo(ParentOrderStatus.ACTIVE);
        regeneratePlan();
        staleSince = -1L;
        degraded = false;
    }

    public synchronized void cancel() {
        if (parent.getStatus().isTerminal()) {
            return;
        }
        transitionTo(ParentOrderStatus.CANCELLING);
        cancelAllActiveChildren();
    }

    public synchronized void markFailed(RuntimeException error) {
        lastError = error.getMessage();
        DeviationRecord record = new DeviationRecord(parent.getId(), engine.clock().now(),
                -1, 0L, parent.getFilledQty(), 0L, "EXECUTOR_ERROR:" + lastError);
        engine.store().recordDeviation(parent.getId(), record);
        cancelAllActiveChildren();
        transitionTo(ParentOrderStatus.PAUSED);
    }

    public synchronized void onTick(long now) {
        if (finished || parent.getStatus().isTerminal()) {
            finished = true;
            engine.removeExecutor(parent.getId());
            return;
        }

        refreshAggregates();

        switch (parent.getStatus()) {
            case CANCELLING -> handleCancelling();
            case PAUSED -> {
            }
            case ACTIVE -> handleActive(now);
            default -> {
            }
        }

        refreshAggregates();
        checkCompletion(now);
    }

    public boolean isFinished() {
        return finished;
    }

    private void handleActive(long now) {
        List<ChildOrder> activeChildren = activeChildren();

        for (ChildOrder child : activeChildren) {
            if (now - child.getSubmittedAt() >= engine.properties().getOrderTimeoutMillis()) {
                long released = engine.matchingEngine().cancel(child);
                if (released > 0) {
                    releaseReservation(released, child);
                }
                engine.store().saveChild(child);
            }
        }

        Quote quote = engine.marketData().latestQuote(parent.getSymbol());
        if (!handleStaleQuote(quote, now)) {
            return;
        }

        if (plan == null || nextSliceIndex >= plan.getSlices().size()) {
            if (now >= parent.getEndTime() && activeChildren().isEmpty()
                    && parent.remainingQty() > 0L) {
                submitCatchUpSlice(now, quote);
            }
            return;
        }

        PlanSlice next = plan.getSlices().get(nextSliceIndex);
        boolean due = parent.getStrategy() == StrategyType.TIME_SCHEDULE
                ? now >= next.scheduledTime()
                : volumeTargetReached(quote, next);
        if (due) {
            submitSlice(next, quote, now);
        }

        recordDeviationIfNeeded(quote, now);
    }

    private void handleCancelling() {
        cancelAllActiveChildren();
        if (activeChildren().isEmpty()) {
            transitionTo(ParentOrderStatus.CANCELLED);
        }
    }

    private boolean handleStaleQuote(Quote quote, long now) {
        long actionThreshold = engine.properties().getStaleQuoteMillis();
        if (actionThreshold <= 0L) {
            return true;
        }
        if (quote == null) {
            return false;
        }
        long quoteAge = now - quote.timestamp();
        if (quoteAge < actionThreshold) {
            staleSince = -1L;
            degraded = false;
            return true;
        }
        if (staleSince < 0L) {
            staleSince = now - quoteAge;
        }
        String action = engine.properties().getStaleQuoteAction();
        if ("PAUSE".equalsIgnoreCase(action) && parent.getStatus() == ParentOrderStatus.ACTIVE) {
            cancelAllActiveChildren();
            transitionTo(ParentOrderStatus.PAUSED);
            return false;
        }
        if ("DEGRADE".equalsIgnoreCase(action)) {
            degraded = true;
            return false;
        }
        return true;
    }

    private boolean volumeTargetReached(Quote quote, PlanSlice slice) {
        if (volumeBaseline < 0L) {
            volumeBaseline = quote.cumulativeMarketVolume();
        }
        long plannedCum = 0L;
        for (int i = 0; i <= slice.index() && i < plan.getSlices().size(); i++) {
            plannedCum += plan.getSlices().get(i).plannedQty();
        }
        long target = ((VolumeParticipationPlanner) engine.planner(StrategyType.VOLUME_PARTICIPATION))
                .targetCumQty(volumeBaseline, quote.cumulativeMarketVolume(),
                        parent.getParticipationRate(), parent.remainingQty() + parent.getFilledQty());
        return target >= plannedCum;
    }

    private void submitSlice(PlanSlice slice, Quote quote, long now) {
        if (degraded) {
            return;
        }
        long qty = slice.plannedQty();
        if (qty <= 0L || parent.remainingQty() <= 0L) {
            nextSliceIndex++;
            return;
        }
        qty = Math.min(qty, parent.remainingQty());
        ChildOrder child = newChild(slice.index(), qty, now);
        if (!reserveAndSubmit(child, qty, quote)) {
            nextSliceIndex++;
            return;
        }
        nextSliceIndex++;
        recordDeviation(quote, now, slice.index());
    }

    private void submitCatchUpSlice(long now, Quote quote) {
        if (degraded) {
            return;
        }
        long remaining = parent.remainingQty();
        if (remaining <= 0L || !activeChildren().isEmpty()) {
            return;
        }
        ChildOrder child = newChild(-1, remaining, now);
        if (!reserveAndSubmit(child, remaining, quote)) {
            transitionTo(ParentOrderStatus.EXPIRED);
        }
    }

    private ChildOrder newChild(int sliceIndex, long qty, long now) {
        long childId = childIdSequence.incrementAndGet();
        ChildOrder child = new ChildOrder();
        child.setId(parent.getId() + "-C" + String.format("%06d", childId));
        child.setParentOrderId(parent.getId());
        child.setSymbol(parent.getSymbol());
        child.setSliceIndex(sliceIndex);
        child.setPlanVersion(parent.getPlanVersion());
        child.setQty(qty);
        child.setFilledQty(0L);
        child.setSubmittedTick(engine.clock().currentTick());
        child.setSubmittedAt(now);
        child.setLastUpdated(now);
        child.setStatus(ChildOrderStatus.PENDING);
        return child;
    }

    private boolean reserveAndSubmit(ChildOrder child, long qty, Quote quote) {
        long price = quote != null ? quote.price() : 100L;
        if (!engine.account().tryReserve(parent.getId(), parent.getSymbol(), parent.getSide(), qty, price)) {
            DeviationRecord record = new DeviationRecord(parent.getId(), engine.clock().now(),
                    child.getSliceIndex(), qty, 0L, -qty, "ACCOUNT_LIMIT_REJECTED");
            engine.store().recordDeviation(parent.getId(), record);
            return false;
        }
        engine.store().saveChild(child);
        Quote effectiveQuote = quote != null ? quote
                : new Quote(parent.getSymbol(), price, 0L, engine.clock().now(), 0L);
        for (Fill fill : engine.matchingEngine().submit(child, effectiveQuote)) {
            engine.account().onFill(parent.getId(), parent.getSymbol(), parent.getSide(),
                    fill.qty(), fill.price());
        }
        engine.store().saveChild(child);
        parent.setSubmittedQty(parent.getSubmittedQty() + qty);
        persistParent();
        return true;
    }

    private void cancelAllActiveChildren() {
        for (ChildOrder child : engine.store().childrenOf(parent.getId())) {
            if (child.getStatus().isActive()) {
                long released = engine.matchingEngine().cancel(child);
                if (released > 0L) {
                    releaseReservation(released, child);
                }
                engine.store().saveChild(child);
            }
        }
    }

    private void releaseReservation(long qty, ChildOrder child) {
        long price = child.getAvgPrice() > 0L ? child.getAvgPrice() : 100L;
        engine.account().releaseReserved(parent.getId(), parent.getSymbol(), parent.getSide(), qty, price);
    }

    private List<ChildOrder> activeChildren() {
        return engine.store().childrenOf(parent.getId()).stream()
                .filter(child -> child.getStatus().isActive())
                .toList();
    }

    private void refreshAggregates() {
        long filled = 0L;
        for (ChildOrder child : engine.store().childrenOf(parent.getId())) {
            filled += child.getFilledQty();
        }
        parent.setFilledQty(Math.min(filled, parent.getTotalQty()));
        persistParent();
    }

    private void checkCompletion(long now) {
        if (parent.getStatus() == ParentOrderStatus.CANCELLING) {
            if (activeChildren().isEmpty()) {
                transitionTo(ParentOrderStatus.CANCELLED);
            }
            return;
        }
        if (parent.getStatus() != ParentOrderStatus.ACTIVE) {
            return;
        }
        if (parent.remainingQty() <= 0L) {
            transitionTo(ParentOrderStatus.COMPLETED);
            return;
        }
        if (now >= parent.getEndTime() && plan != null
                && nextSliceIndex >= plan.getSlices().size() && activeChildren().isEmpty()) {
            transitionTo(ParentOrderStatus.EXPIRED);
        }
    }

    private void regeneratePlan() {
        int version = parent.getPlanVersion() + 1;
        parent.setPlanVersion(version);
        Quote quote = engine.marketData().latestQuote(parent.getSymbol());
        PlanContext context = new PlanContext(parent, engine.clock().now(), version, quote);
        ExecutionPlan newPlan = engine.planner(parent.getStrategy()).buildPlan(context);
        plan = newPlan;
        nextSliceIndex = 0;
        volumeBaseline = -1L;
        degraded = false;
        engine.store().savePlan(newPlan);
        persistParent();
    }

    private void recordDeviationIfNeeded(Quote quote, long now) {
        if (plan == null || now == lastDeviationTick) {
            return;
        }
        int dueSlices = 0;
        for (PlanSlice slice : plan.getSlices()) {
            boolean due = parent.getStrategy() == StrategyType.TIME_SCHEDULE
                    ? now >= slice.scheduledTime()
                    : volumeTargetReached(quote, slice);
            if (due) {
                dueSlices++;
            }
        }
        long plannedCum = 0L;
        for (int i = 0; i < dueSlices && i < plan.getSlices().size(); i++) {
            plannedCum += plan.getSlices().get(i).plannedQty();
        }
        long actualCum = parent.getFilledQty();
        if (plannedCum != actualCum) {
            DeviationRecord record = new DeviationRecord(parent.getId(), now,
                    dueSlices == 0 ? -1 : plan.getSlices().get(dueSlices - 1).index(),
                    plannedCum, actualCum, actualCum - plannedCum,
                    "planned-vs-filled@tick" + engine.clock().currentTick());
            engine.store().recordDeviation(parent.getId(), record);
            lastDeviationTick = now;
        }
    }

    private void recordDeviation(Quote quote, long now, int sliceIndex) {
        long plannedCum = 0L;
        for (int i = 0; i <= sliceIndex && i < plan.getSlices().size(); i++) {
            plannedCum += plan.getSlices().get(i).plannedQty();
        }
        long actualCum = parent.getFilledQty();
        DeviationRecord record = new DeviationRecord(parent.getId(), now, sliceIndex,
                plannedCum, actualCum, actualCum - plannedCum, "slice-submitted");
        engine.store().recordDeviation(parent.getId(), record);
    }

    private void transitionTo(ParentOrderStatus newStatus) {
        if (parent.getStatus() == newStatus || parent.getStatus().isTerminal()) {
            return;
        }
        parent.setStatus(newStatus);
        persistParent();
    }

    private void persistParent() {
        engine.store().saveParent(parent);
    }

    private long parseChildId(String childId) {
        try {
            return Long.parseLong(childId.substring(childId.lastIndexOf('C') + 1));
        } catch (RuntimeException e) {
            return 0L;
        }
    }
}
