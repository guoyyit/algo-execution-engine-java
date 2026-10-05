package com.github.guoyyit.algoexecutionenginejava.execution;

import com.github.guoyyit.algoexecutionenginejava.model.ExecutionEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryEventPublisher implements EventPublisher {

    private final CopyOnWriteArrayList<ExecutionEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public void publish(ExecutionEvent event) {
        events.add(event);
    }

    @Override
    public List<ExecutionEvent> drain() {
        return new ArrayList<>(events);
    }

    public List<ExecutionEvent> eventsFor(String parentOrderId) {
        List<ExecutionEvent> result = new ArrayList<>();
        for (ExecutionEvent event : events) {
            if (parentOrderId.equals(event.getParentOrderId())) {
                result.add(event);
            }
        }
        return result;
    }
}
