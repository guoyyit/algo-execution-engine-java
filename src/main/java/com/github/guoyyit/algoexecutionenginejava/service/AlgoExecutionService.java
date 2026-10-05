package com.github.guoyyit.algoexecutionenginejava.service;

import com.github.guoyyit.algoexecutionenginejava.model.ExecutionEvent;
import com.github.guoyyit.algoexecutionenginejava.model.MarketTick;
import com.github.guoyyit.algoexecutionenginejava.model.OrderProgress;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrder;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrderRequest;

import java.util.List;

public interface AlgoExecutionService {

    ParentOrder submit(ParentOrderRequest request);

    void pause(String parentOrderId);

    void resume(String parentOrderId);

    void cancel(String parentOrderId);

    OrderProgress query(String parentOrderId);

    List<OrderProgress> list();

    void onMarketTick(MarketTick tick);

    List<ExecutionEvent> events(String parentOrderId);
}
