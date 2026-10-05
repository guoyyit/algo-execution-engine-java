package com.github.guoyyit.algoexecutionenginejava.execution;

import com.github.guoyyit.algoexecutionenginejava.model.ExecutionEvent;

import java.util.List;

public interface EventPublisher {

    void publish(ExecutionEvent event);

    List<ExecutionEvent> drain();
}
