package com.github.guoyyit.algoexecutionenginejava.persist;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.nio.file.Path;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.ArrayList;

public class FileEventLog implements EventLog {

    private final Path logFile;
    private final ObjectMapper objectMapper;
    private long sequence;

    public FileEventLog(Path logDir, ObjectMapper objectMapper) {
        try {
            Files.createDirectories(logDir);
            this.logFile = logDir.resolve("events.jsonl");
            if (Files.notExists(logFile)) {
                Files.createFile(logFile);
            }
            this.sequence = 0L;
        } catch (IOException e) {
            throw new UncheckedIOException("cannot init event log at " + logDir, e);
        }
        this.objectMapper = objectMapper;
    }

    @Override
    public synchronized void append(String type, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            EventRecord record = new EventRecord(++sequence, System.currentTimeMillis(), type, payloadJson);
            String line = objectMapper.writeValueAsString(record) + System.lineSeparator();
            Files.writeString(logFile, line, StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.APPEND,
                    java.nio.file.StandardOpenOption.SYNC);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot append event " + type, e);
        }
    }

    @Override
    public List<EventRecord> readAll() {
        List<EventRecord> records = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(logFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    records.add(objectMapper.readValue(line, EventRecord.class));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read event log", e);
        }
        return records;
    }

    @Override
    public synchronized long lastSequence() {
        return sequence;
    }

    public synchronized void writeTickWatermark(long tickCount) {
        Path watermark = logFile.resolveSibling("tick.watermark");
        try {
            Files.writeString(watermark, Long.toString(tickCount), StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                    java.nio.file.StandardOpenOption.SYNC);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write tick watermark", e);
        }
    }

    public long readTickWatermark() {
        Path watermark = logFile.resolveSibling("tick.watermark");
        try {
            if (Files.notExists(watermark)) {
                return 0L;
            }
            String content = Files.readString(watermark, StandardCharsets.UTF_8).trim();
            return content.isBlank() ? 0L : Long.parseLong(content);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read tick watermark", e);
        }
    }

    public void writeJsonState(String fileName, Object state) {
        try {
            Files.writeString(logFile.resolveSibling(fileName),
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(state),
                    StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                    java.nio.file.StandardOpenOption.SYNC);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write state " + fileName, e);
        }
    }

    public <T> T readJsonState(String fileName, Class<T> type) {
        Path file = logFile.resolveSibling(fileName);
        try {
            if (Files.notExists(file)) {
                return null;
            }
            return objectMapper.readValue(Files.readString(file, StandardCharsets.UTF_8), type);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read state " + fileName, e);
        }
    }
}
