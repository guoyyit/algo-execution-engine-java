package com.github.guoyyit.algoexecutionenginejava.execution;

import com.github.guoyyit.algoexecutionenginejava.market.FillResult;
import com.github.guoyyit.algoexecutionenginejava.market.MarketDataFeed;
import com.github.guoyyit.algoexecutionenginejava.market.MatchingEngine;
import com.github.guoyyit.algoexecutionenginejava.market.SimClock;
import com.github.guoyyit.algoexecutionenginejava.model.ChildOrder;
import com.github.guoyyit.algoexecutionenginejava.model.ChildStatus;
import com.github.guoyyit.algoexecutionenginejava.model.EventType;
import com.github.guoyyit.algoexecutionenginejava.model.ExecutionPlan;
import com.github.guoyyit.algoexecutionenginejava.model.ExecutionEvent;
import com.github.guoyyit.algoexecutionenginejava.model.MarketTick;
import com.github.guoyyit.algoexecutionenginejava.model.OrderProgress;
import com.github.guoyyit.algoexecutionenginejava.model.PlanDeviation;
import com.github.guoyyit.algoexecutionenginejava.model.ParentStatus;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrder;
import com.github.guoyyit.algoexecutionenginejava.model.Slice;
import com.github.guoyyit.algoexecutionenginejava.model.StaleQuotePolicy;
import com.github.guoyyit.algoexecutionenginejava.model.StrategyType;
import com.github.guoyyit.algoexecutionenginejava.strategy.PlanContext;
import com.github.guoyyit.algoexecutionenginejava.strategy.SlicePlanner;

import java.util.Map;
import java.util.Random;

public class ParentOrderExecutor {

    private final ParentOrder parent;
    private final Map<StrategyType, SlicePlanner> planners;
    private final MatchingEngine matchingEngine;
    private final MarketDataFeed feed;
    private final Account account;
    private final OrderStore store;
    private final SimClock clock;
    private final IdGenerator idGenerator;
    private final EventPublisher publisher;
    private final Random random;

    private boolean degraded;

    public ParentOrderExecutor(ParentOrder parent, Map<StrategyType, SlicePlanner> planners,
                               MatchingEngine matchingEngine, MarketDataFeed feed,
                               Account account, OrderStore store, SimClock clock,
                               IdGenerator idGenerator, EventPublisher publisher) {
        this.parent = parent;
        this.planners = planners;
        this.matchingEngine = matchingEngine;
        this.feed = feed;
        this.account = account;
        this.store = store;
        this.clock = clock;
        this.idGenerator = idGenerator;
        this.publisher = publisher;
        this.random = new Random(parent.getRandomSeed());
    }

    public ParentOrder getParent() {
        return parent;
    }

    public synchronized void initialize() {
        if (parent.getStatus() == ParentStatus.CREATED) {
            parent.setStatus(ParentStatus.ACTIVE);
            parent.setNextSliceIndex(0);
            generatePlan();
            persist();
            publishSimple(EventType.ORDER_ACCEPTED, parent.getTotalQuantity(), 0.0, "order accepted");
        }
    }

    public synchronized void pause() {
        if (parent.getStatus().isTerminal() || parent.getStatus() == ParentStatus.PAUSED) {
            return;
        }
        parent.setStatus(ParentStatus.PAUSED);
        parent.setLastUpdateTime(clock.millis());
        persist();
        publishSimple(EventType.PAUSED, 0L, 0.0, "paused by client or stale-quote policy");
    }

    public synchronized void resume() {
        if (parent.getStatus() != ParentStatus.PAUSED) {
            return;
        }
        parent.setStatus(ParentStatus.ACTIVE);
        parent.setLastUpdateTime(clock.millis());
        parent.setNextSliceIndex(0);
        degraded = false;
        generatePlan();
        persist();
        publishSimple(EventType.RESUMED, 0L, 0.0,
                "resumed; plan regenerated for remaining quantity " + parent.getOutstandingQuantity());
    }

    public synchronized void cancel() {
        if (parent.getStatus().isTerminal()) {
            return;
        }
        cancelAllLiveChildren("cancelled by client");
        parent.setStatus(ParentStatus.CANCELLED);
        parent.setLastUpdateTime(clock.millis());
        persist();
        publishSimple(EventType.CANCELLED, 0L, 0.0, "cancelled; all live children cancelled");
    }

    public synchronized void onTick(MarketTick tick) {
        if (tick != null && tick.getSymbol().equals(parent.getSymbol())) {
            pump(clock.millis());
        }
    }

    public synchronized void onTimeElapsed(long now) {
        pump(now);
    }

    public synchronized void applyRecoveredEvent(ExecutionEvent event) {
        ChildOrder child = event.getChildOrderId() == null ? null : parent.findChild(event.getChildOrderId());
        switch (event.getType()) {
            case CHILD_SUBMITTED -> {
                if (child != null && child.getStatus() == ChildStatus.PENDING) {
                    child.setStatus(ChildStatus.SUBMITTED);
                }
            }
            case CHILD_FILL -> {
                if (child != null && child.isLive() && event.getQuantity() > child.getFilledQuantity()) {
                    parent.applyChildFill(child, event.getQuantity() - child.getFilledQuantity(),
                            event.getPrice(), event.getTime());
                }
            }
            case CHILD_CANCELLED -> markRecoveredTerminal(child, ChildStatus.CANCELLED);
            case CHILD_EXPIRED -> markRecoveredTerminal(child, ChildStatus.EXPIRED);
            case CHILD_REJECTED -> markRecoveredTerminal(child, ChildStatus.REJECTED);
            case PAUSED -> parent.setStatus(ParentStatus.PAUSED);
            case CANCELLED -> parent.setStatus(ParentStatus.CANCELLED);
            case EXPIRED -> parent.setStatus(ParentStatus.EXPIRED);
            case COMPLETED -> parent.setStatus(ParentStatus.COMPLETED);
            default -> {
            }
        }
    }

    public synchronized OrderProgress progress() {
        OrderProgress progress = new OrderProgress();
        progress.setParentOrderId(parent.getParentOrderId());
        progress.setClientOrderId(parent.getClientOrderId());
        progress.setStatus(parent.getStatus());
        progress.setSymbol(parent.getSymbol());
        progress.setSide(parent.getSide());
        progress.setTotalQuantity(parent.getTotalQuantity());
        progress.setCumulativeFilled(parent.getCumulativeFilled());
        progress.setRemainingQuantity(parent.getOutstandingQuantity());
        progress.setLiveQuantity(parent.getLiveQuantity());
        progress.setProgressRatio(parent.getTotalQuantity() == 0 ? 0.0
                : (double) parent.getCumulativeFilled() / parent.getTotalQuantity());
        progress.setAverageFillPrice(parent.getAverageFillPrice());
        progress.setFaultReason(parent.getFaultReason());
        ExecutionPlan plan = parent.getCurrentPlan();
        if (plan != null) {
            progress.setPlanVersion(plan.getVersion());
            long plannedCum = plan.plannedCumQuantityByTime(clock.millis());
            progress.setPlannedCumQuantity(plannedCum);
            progress.setDeviationQuantity(plannedCum - parent.getCumulativeFilled());
        }
        return progress;
    }

    private void markRecoveredTerminal(ChildOrder child, ChildStatus status) {
        if (child != null && child.isLive()) {
            account.release(parent.getParentOrderId(), parent.getSide(), child.getOutstandingQuantity());
            child.setStatus(status);
        } else if (child != null) {
            child.setStatus(status);
        }
    }

    private void pump(long now) {
        if (parent.getStatus().isTerminal()) {
            return;
        }
        try {
            if (now >= parent.getEndTime()) {
                expireOrComplete(now);
                return;
            }
            handleStaleQuote(now);
            if (parent.getStatus() == ParentStatus.PAUSED || parent.getStatus().isTerminal()) {
                return;
            }
            matchLiveChildren(now);
            submitDueSlices(now);
            recordDeviation(now, "pump");
            checkCompleted(now);
        } catch (RuntimeException e) {
            fault("execution error: " + e.getMessage());
        }
    }

    private void fault(String reason) {
        cancelAllLiveChildren("fault: " + reason);
        parent.setStatus(ParentStatus.FAILED);
        parent.setFaultReason(reason);
        parent.setLastUpdateTime(clock.millis());
        persist();
        publishSimple(EventType.FAULT, 0L, 0.0, reason);
    }

    private void generatePlan() {
        SlicePlanner planner = planners.get(parent.getStrategy());
        if (planner == null) {
            throw new IllegalStateException("no planner for " + parent.getStrategy());
        }
        PlanContext context = new PlanContext();
        context.setStrategy(parent.getStrategy());
        context.setNow(clock.millis());
        context.setWindowStart(clock.millis());
        context.setWindowEnd(parent.getEndTime());
        context.setSliceCount(parent.getSliceCount());
        context.setParticipationRate(parent.getParticipationRate());
        context.setRemainingQuantity(parent.getOutstandingQuantity());
        ExecutionPlan existing = parent.getCurrentPlan();
        context.setPlanVersion(existing == null ? 1 : existing.getVersion() + 1);
        context.setLatestTick(feed.latest(parent.getSymbol()));
        context.setHistoryTicks(feed.history(parent.getSymbol()));
        ExecutionPlan plan = planner.buildPlan(context);
        parent.setCurrentPlan(plan);
        parent.setLastUpdateTime(clock.millis());
        publishSimple(EventType.PLAN_GENERATED, plan.totalPlannedQuantity(), 0.0,
                "plan v" + plan.getVersion() + " with " + plan.getSlices().size() + " slices");
    }

    private void submitDueSlices(long now) {
        ExecutionPlan plan = parent.getCurrentPlan();
        if (plan == null || parent.getNextSliceIndex() >= plan.getSlices().size()) {
            return;
        }
        Slice slice = plan.getSlices().get(parent.getNextSliceIndex());
        if (slice.getPlannedTime() > now) {
            return;
        }
        cancelLiveChildBeforeNextSlice(now);
        long available = parent.getOutstandingQuantity() - parent.getLiveQuantity();
        long quantity = Math.min(slice.getPlannedQuantity(), available);
        if (degraded) {
            quantity /= 2;
        }
        if (quantity <= 0) {
            parent.setNextSliceIndex(parent.getNextSliceIndex() + 1);
            submitDueSlices(now);
            return;
        }
        if (!account.tryReserve(parent.getParentOrderId(), parent.getSide(), quantity)) {
            publishSimple(EventType.CHILD_REJECTED, quantity, 0.0,
                    "account limit exceeded; waiting for capacity");
            return;
        }
        ChildOrder child = new ChildOrder();
        child.setChildOrderId(idGenerator.nextChildId(parent.getParentOrderId()));
        child.setParentOrderId(parent.getParentOrderId());
        child.setSymbol(parent.getSymbol());
        child.setSide(parent.getSide());
        child.setPlannedQuantity(quantity);
        child.setFilledQuantity(0L);
        child.setStatus(ChildStatus.SUBMITTED);
        child.setPlanVersion(plan.getVersion());
        child.setSliceIndex(slice.getIndex());
        child.setPlannedTime(slice.getPlannedTime());
        child.setSubmittedAt(now);
        child.setLastUpdateTime(now);
        parent.getChildren().add(child);
        parent.setNextSliceIndex(parent.getNextSliceIndex() + 1);
        persist();
        publishChild(EventType.CHILD_SUBMITTED, child, quantity, 0.0,
                "slice " + slice.getIndex() + " of plan v" + plan.getVersion()
                        + (degraded ? " (degraded half size)" : ""));
        matchLiveChildren(now);
    }

    private void matchLiveChildren(long now) {
        MarketTick tick = feed.latest(parent.getSymbol());
        if (tick == null) {
            return;
        }
        for (ChildOrder child : parent.getChildren()) {
            if (!child.isLive()) {
                continue;
            }
            FillResult result = matchingEngine.simulateFill(child, tick, random);
            if (result.getFilledQuantity() > 0) {
                parent.applyChildFill(child, result.getFilledQuantity(), result.getPrice(), now);
                if (child.getStatus() == ChildStatus.FILLED) {
                    account.release(parent.getParentOrderId(), parent.getSide(), 0L);
                }
                persist();
                publishChild(EventType.CHILD_FILL, child, result.getFilledQuantity(), result.getPrice(),
                        child.getStatus() == ChildStatus.FILLED ? "child fully filled" : "child partially filled");
            }
        }
    }

    private void cancelLiveChildBeforeNextSlice(long now) {
        for (ChildOrder child : parent.getChildren()) {
            if (child.isLive()) {
                long outstanding = child.getOutstandingQuantity();
                account.release(parent.getParentOrderId(), parent.getSide(), outstanding);
                ChildStatus status = child.getFilledQuantity() == 0 ? ChildStatus.CANCELLED : ChildStatus.EXPIRED;
                child.setStatus(status);
                child.setLastUpdateTime(now);
                publishChild(status == ChildStatus.CANCELLED ? EventType.CHILD_CANCELLED : EventType.CHILD_EXPIRED,
                        child, outstanding, 0.0, "slice ended; unfilled quantity returned to remaining");
            }
        }
    }

    private void cancelAllLiveChildren(String reason) {
        long now = clock.millis();
        for (ChildOrder child : parent.getChildren()) {
            if (child.isLive()) {
                long outstanding = child.getOutstandingQuantity();
                account.release(parent.getParentOrderId(), parent.getSide(), outstanding);
                child.setStatus(ChildStatus.CANCELLED);
                child.setLastUpdateTime(now);
                publishChild(EventType.CHILD_CANCELLED, child, outstanding, 0.0, reason);
            }
        }
        persist();
    }

    private void handleStaleQuote(long now) {
        MarketTick tick = feed.latest(parent.getSymbol());
        long timeout = parent.getStaleQuoteTimeoutMillis();
        long lastQuoteTime = tick == null ? parent.getStartTime() : tick.getTime();
        if (now - lastQuoteTime <= timeout) {
            degraded = false;
            return;
        }
        publishSimple(EventType.STALE_QUOTE, 0L, 0.0,
                "no quote for " + (now - lastQuoteTime) + "ms; policy=" + parent.getStaleQuotePolicy());
        if (parent.getStaleQuotePolicy() == StaleQuotePolicy.PAUSE) {
            pause();
        } else {
            degraded = true;
        }
    }

    private void expireOrComplete(long now) {
        cancelAllLiveChildren("parent order window expired");
        if (parent.getCumulativeFilled() >= parent.getTotalQuantity()) {
            parent.setStatus(ParentStatus.COMPLETED);
            publishSimple(EventType.COMPLETED, parent.getCumulativeFilled(), parent.getAverageFillPrice(),
                    "completed at end of window");
        } else {
            parent.setStatus(ParentStatus.EXPIRED);
            publishSimple(EventType.EXPIRED, parent.getOutstandingQuantity(), 0.0,
                    "expired with unfilled quantity " + parent.getOutstandingQuantity());
        }
        parent.setLastUpdateTime(now);
        persist();
    }

    private void checkCompleted(long now) {
        if (parent.getCumulativeFilled() >= parent.getTotalQuantity()) {
            cancelAllLiveChildren("parent order fully filled");
            parent.setStatus(ParentStatus.COMPLETED);
            parent.setLastUpdateTime(now);
            persist();
            publishSimple(EventType.COMPLETED, parent.getCumulativeFilled(), parent.getAverageFillPrice(),
                    "fully filled");
        }
    }

    private void recordDeviation(long now, String reason) {
        ExecutionPlan plan = parent.getCurrentPlan();
        if (plan == null || hasDeviationForTime(now)) {
            return;
        }
        long plannedCum = plan.plannedCumQuantityByTime(now);
        long deviation = plannedCum - parent.getCumulativeFilled();
        Slice lastSlice = null;
        for (Slice slice : plan.getSlices()) {
            if (slice.getPlannedTime() <= now) {
                lastSlice = slice;
            }
        }
        PlanDeviation record = new PlanDeviation();
        record.setTime(now);
        record.setPlanVersion(plan.getVersion());
        record.setSliceIndex(lastSlice == null ? -1 : lastSlice.getIndex());
        record.setPlannedCumQuantity(plannedCum);
        record.setActualCumFilled(parent.getCumulativeFilled());
        record.setQuantityDeviation(deviation);
        record.setPlannedTime(lastSlice == null ? now : lastSlice.getPlannedTime());
        record.setTimeDeviationMillis(now - (lastSlice == null ? now : lastSlice.getPlannedTime()));
        record.setReason(reason);
        parent.getDeviations().add(record);
    }

    private boolean hasDeviationForTime(long now) {
        return parent.getDeviations().stream()
                .anyMatch(deviation -> deviation.getTime() == now);
    }

    private void persist() {
        store.saveParent(parent);
    }

    private void publishSimple(EventType type, long quantity, double price, String detail) {
        ExecutionEvent event = new ExecutionEvent();
        event.setSequence(idGenerator.nextEventSequence());
        event.setTime(clock.millis());
        event.setParentOrderId(parent.getParentOrderId());
        event.setType(type);
        event.setQuantity(quantity);
        event.setPrice(price);
        event.setDetail(detail);
        publisher.publish(event);
        store.appendEvent(event);
    }

    private void publishChild(EventType type, ChildOrder child, long quantity, double price, String detail) {
        ExecutionEvent event = new ExecutionEvent();
        event.setSequence(idGenerator.nextEventSequence());
        event.setTime(clock.millis());
        event.setParentOrderId(parent.getParentOrderId());
        event.setChildOrderId(child.getChildOrderId());
        event.setType(type);
        event.setQuantity(quantity);
        event.setPrice(price);
        event.setDetail(detail);
        publisher.publish(event);
        store.appendEvent(event);
    }
}
