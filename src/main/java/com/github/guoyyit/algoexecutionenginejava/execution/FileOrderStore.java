package com.github.guoyyit.algoexecutionenginejava.execution;

import com.github.guoyyit.algoexecutionenginejava.model.ExecutionEvent;
import com.github.guoyyit.algoexecutionenginejava.model.ParentOrder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FileOrderStore implements OrderStore {

    private final Path directory;
    private final Path eventsFile;
    private final ObjectMapper mapper = new ObjectMapper();

    public FileOrderStore(Path directory) {
        this.directory = directory;
        this.eventsFile = directory.resolve("events.log");
        try {
            Files.createDirectories(directory);
            if (Files.notExists(eventsFile)) {
                Files.createFile(eventsFile);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot initialize store at " + directory, e);
        }
    }

    @Override
    public void saveParent(ParentOrder parent) {
        Path target = directory.resolve(parent.getParentOrderId() + ".json");
        Path temp = directory.resolve(parent.getParentOrderId() + ".json.tmp");
        try {
            synchronized (ioLock(parent.getParentOrderId())) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), parent);
                Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot save parent " + parent.getParentOrderId(), e);
        }
    }

    @Override
    public void appendEvent(ExecutionEvent event) {
        try {
            synchronized (eventsFile) {
                Files.writeString(eventsFile, mapper.writeValueAsString(event) + System.lineSeparator(),
                        java.nio.file.StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot append event " + event.getSequence(), e);
        }
    }

    @Override
    public List<ParentOrder> loadParents() {
        List<ParentOrder> result = new ArrayList<>();
        try (Stream<Path> paths = Files.list(directory)) {
            paths.filter(path -> path.getFileName().toString().endsWith(".json")).forEach(path -> {
                try {
                    result.add(mapper.readValue(path.toFile(), ParentOrder.class));
                } catch (IOException e) {
                    throw new UncheckedIOException("cannot read " + path, e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("cannot list store directory", e);
        }
        return result;
    }

    @Override
    public List<ExecutionEvent> loadEvents(String parentOrderId) {
        List<ExecutionEvent> all = readAllEvents();
        List<ExecutionEvent> result = new ArrayList<>();
        for (ExecutionEvent event : all) {
            if (parentOrderId.equals(event.getParentOrderId())) {
                result.add(event);
            }
        }
        return result;
    }

    @Override
    public long lastEventSequence() {
        long max = 0L;
        for (ExecutionEvent event : readAllEvents()) {
            max = Math.max(max, event.getSequence());
        }
        return max;
    }

    @Override
    public void reset() {
        try (Stream<Path> paths = Files.list(directory)) {
            paths.forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException("cannot delete " + path, e);
                }
            });
            Files.createFile(eventsFile);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot reset store", e);
        }
    }

    private List<ExecutionEvent> readAllEvents() {
        List<ExecutionEvent> result = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(eventsFile)) {
                if (!line.isBlank()) {
                    result.add(mapper.readValue(line, ExecutionEvent.class));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read event log", e);
        }
        return result;
    }

    private Object ioLock(String key) {
        return key.intern();
    }
}
