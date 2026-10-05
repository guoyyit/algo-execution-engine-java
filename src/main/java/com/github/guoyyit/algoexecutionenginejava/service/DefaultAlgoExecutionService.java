package com.github.guoyyit.algoexecutionenginejava.service;

import com.github.guoyyit.algoexecutionenginejava.execution.ExecutionEngine;
import com.github.guoyyit.algoexecutionenginejava.model.ExecutionEvent;
import com.github.guoyyit.algoexecutionenginejava.model.MarketTick;
import com.github.guoyyit.algoexecutionenginejava.model.OrderProgress;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrder;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrderRequest;

import java.util.List;

public class DefaultAlgoExecutionService implements AlgoExecutionService {

    private final ExecutionEngine engine;

    public DefaultAlgoExecutionService(ExecutionEngine engine) {
        this.engine = engine;
    }

    @Override
    public ParentOrder submit(ParentOrderRequest request) {
        try {
            return engine.submit(request);
        } catch (OrderServiceException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new OrderServiceException("failed to submit order: " + e.getMessage(), e);
        }
    }

    @Override
    public void pause(String parentOrderId) {
        engine.pause(parentOrderId);
    }

    @Override
    public void resume(String parentOrderId) {
        engine.resume(parentOrderId);
    }

    @Override
    public void cancel(String parentOrderId) {
        engine.cancel(parentOrderId);
    }

    @Override
    public OrderProgress query(String parentOrderId) {
        return engine.getProgress(parentOrderId);
    }

    @Override
    public List<OrderProgress> list() {
        return engine.listProgress();
    }

    @Override
    public void onMarketTick(MarketTick tick) {
        engine.onTick(tick);
    }

    @Override
    public List<ExecutionEvent> events(String parentOrderId) {
        return engine.events(parentOrderId);
    }
}
