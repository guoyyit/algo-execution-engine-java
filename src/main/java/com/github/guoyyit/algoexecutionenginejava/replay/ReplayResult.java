package com.github.guoyyit.algoexecutionenginejava.replay;

import com.github.guoyyit.algoexecutionenginejava.model.ExecutionEvent;
import com.github.guoyyit.algoexecutionenginejava.model.OrderProgress;

import java.util.ArrayList;
import java.util.List;

public class ReplayResult {
    private OrderProgress finalProgress;
    private List<ExecutionEvent> events = new ArrayList<>();

    public OrderProgress getFinalProgress() { return finalProgress; }
    public void setFinalProgress(OrderProgress finalProgress) { this.finalProgress = finalProgress; }
    public List<ExecutionEvent> getEvents() { return events; }
    public void setEvents(List<ExecutionEvent> events) { this.events = events; }
}
