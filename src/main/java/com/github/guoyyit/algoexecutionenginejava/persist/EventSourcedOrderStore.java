package com.github.guoyyit.algoexecutionenginejava.persist;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.guoyyit.algoexecutionenginejava.model.ChildOrder;
import com.github.guoyyit.algoexecutionenginejava.model.DeviationRecord;
import com.github.guoyyit.algoexecutionenginejava.model.ExecutionPlan;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrder;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

public class EventSourcedOrderStore implements OrderStore {

    private final EventLog eventLog;
    private final ObjectMapper objectMapper;
    private final Map<String, ParentOrder> parents = new ConcurrentHashMap<>();
    private final Map<String, ChildOrder> children = new ConcurrentHashMap<>();
    private final Map<String, ExecutionPlan> latestPlans = new ConcurrentHashMap<>();
    private final Map<String, List<DeviationRecord>> deviations = new ConcurrentHashMap<>();

    public EventSourcedOrderStore(EventLog eventLog, ObjectMapper objectMapper) {
        this.eventLog = eventLog;
        this.objectMapper = objectMapper;
        replay();
    }

    @Override
    public void saveParent(ParentOrder parent) {
        eventLog.append("PARENT_SAVED", parent);
        parents.put(parent.getId(), parent);
    }

    @Override
    public Optional<ParentOrder> getParent(String parentId) {
        return Optional.ofNullable(parents.get(parentId));
    }

    @Override
    public List<ParentOrder> allParents() {
        return new ArrayList<>(parents.values());
    }

    @Override
    public void saveChild(ChildOrder child) {
        eventLog.append("CHILD_SAVED", child);
        children.put(child.getId(), child);
    }

    @Override
    public Optional<ChildOrder> getChild(String childId) {
        return Optional.ofNullable(children.get(childId));
    }

    @Override
    public List<ChildOrder> childrenOf(String parentId) {
        List<ChildOrder> result = new ArrayList<>();
        for (ChildOrder child : children.values()) {
            if (child.getParentOrderId().equals(parentId)) {
                result.add(child);
            }
        }
        return result;
    }

    @Override
    public void savePlan(ExecutionPlan plan) {
        eventLog.append("PLAN_SAVED", plan);
        latestPlans.put(plan.getParentOrderId(), plan);
    }

    @Override
    public Optional<ExecutionPlan> latestPlan(String parentId) {
        return Optional.ofNullable(latestPlans.get(parentId));
    }

    @Override
    public List<DeviationRecord> deviations(String parentId) {
        return List.copyOf(deviations.getOrDefault(parentId, List.of()));
    }

    @Override
    public void recordDeviation(String parentOrderId, DeviationRecord deviation) {
        eventLog.append("DEVIATION", deviation);
        deviations.computeIfAbsent(parentOrderId, key ->
                java.util.Collections.synchronizedList(new ArrayList<>())).add(deviation);
    }

    @Override
    public long nextSeq() {
        return eventLog.lastSequence() + 1;
    }

    public EventLog eventLog() {
        return eventLog;
    }

    private void replay() {
        for (EventRecord record : eventLog.readAll()) {
            apply(record);
        }
    }

    private void apply(EventRecord record) {
        try {
            switch (record.type()) {
                case "PARENT_SAVED" -> {
                    ParentOrder parent = read(record, ParentOrder.class);
                    parents.put(parent.getId(), parent);
                }
                case "CHILD_SAVED" -> {
                    ChildOrder child = read(record, ChildOrder.class);
                    children.put(child.getId(), child);
                }
                case "PLAN_SAVED" -> {
                    ExecutionPlan plan = read(record, ExecutionPlan.class);
                    latestPlans.put(plan.getParentOrderId(), plan);
                }
                case "DEVIATION" -> {
                    DeviationRecord deviation = read(record, DeviationRecord.class);
                    deviations.computeIfAbsent(deviation.parentOrderId(),
                            key -> java.util.Collections.synchronizedList(new ArrayList<>())).add(deviation);
                }
                default -> {
                }
            }
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("cannot replay event " + record.sequence(), e);
        }
    }

    private <T> T read(EventRecord record, Class<T> type) throws JsonProcessingException {
        return objectMapper.readValue(record.payloadJson(), type);
    }
}
