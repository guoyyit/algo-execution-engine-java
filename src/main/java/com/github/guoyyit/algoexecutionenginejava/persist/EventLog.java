package com.github.guoyyit.algoexecutionenginejava.persist;

import java.util.List;

public interface EventLog {

    void append(String type, Object payload);

    List<EventRecord> readAll();

    long lastSequence();
}
