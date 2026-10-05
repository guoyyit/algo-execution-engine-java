package com.github.guoyyit.algoexecutionenginejava.service;

import com.github.guoyyit.algoexecutionenginejava.engine.ExecutionEngine;
import com.github.guoyyit.algoexecutionenginejava.model.ChildOrder;
import com.github.guoyyit.algoexecutionenginejava.model.DeviationRecord;
import com.github.guoyyit.algoexecutionenginejava.model.ExecutionPlan;
import com.github.guoyyit.algoexecutionenginejava.model.OrderRequest;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlgoExecutionService {

    private final ExecutionEngine engine;

    public AlgoExecutionService(ExecutionEngine engine) {
        this.engine = engine;
    }

    public ParentOrder submit(OrderRequest request) {
        return engine.submit(request);
    }

    public ParentOrder pause(String parentOrderId) {
        return engine.pause(parentOrderId);
    }

    public ParentOrder resume(String parentOrderId) {
        return engine.resume(parentOrderId);
    }

    public ParentOrder cancel(String parentOrderId) {
        return engine.cancel(parentOrderId);
    }

    public ParentOrder getParent(String parentOrderId) {
        return engine.getParent(parentOrderId);
    }

    public List<ParentOrder> allParents() {
        return engine.allParents();
    }

    public ExecutionPlan getPlan(String parentOrderId) {
        return engine.store().latestPlan(parentOrderId).orElse(null);
    }

    public List<ChildOrder> getChildren(String parentOrderId) {
        return engine.store().childrenOf(parentOrderId);
    }

    public List<DeviationRecord> getDeviations(String parentOrderId) {
        return engine.store().deviations(parentOrderId);
    }

    public void advanceTicks(int ticks) {
        engine.runTicks(ticks);
    }
}
