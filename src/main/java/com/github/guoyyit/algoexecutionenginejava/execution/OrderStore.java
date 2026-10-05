package com.github.guoyyit.algoexecutionenginejava.execution;

import com.github.guoyyit.algoexecutionenginejava.model.ExecutionEvent;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrder;

import java.util.List;

public interface OrderStore {

    void saveParent(ParentOrder parent);

    void appendEvent(ExecutionEvent event);

    List<ParentOrder> loadParents();

    List<ExecutionEvent> loadEvents(String parentOrderId);

    long lastEventSequence();

    void reset();
}
