package com.github.guoyyit.algoexecutionenginejava.simulation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.guoyyit.algoexecutionenginejava.model.Quote;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class MarketDataSimulator implements MarketDataSource {

    private final SimClock clock;
    private final ConcurrentHashMap<String, SymbolSpec> specs = new ConcurrentHashMap<>();

    public MarketDataSimulator(SimClock clock) {
        this.clock = clock;
    }

    public void registerSymbol(String symbol, long startPrice, long baseVolumePerTick, Random random) {
        registerSymbol(symbol, startPrice, baseVolumePerTick,
                random == null ? 1L : random.nextLong());
    }

    public void registerSymbol(String symbol, long startPrice, long baseVolumePerTick, long seed) {
        specs.putIfAbsent(symbol,
                new SymbolSpec(symbol, startPrice, baseVolumePerTick, seed));
    }

    public void restore(SymbolState state) {
        SymbolSpec spec = new SymbolSpec(state.symbol(), state.startPrice(),
                state.baseVolumePerTick(), state.seed());
        spec.price = state.price();
        spec.cumulativeVolume = state.cumulativeVolume();
        spec.sequence = state.sequence();
        spec.registered = true;
        spec.frozen = state.frozen();
        spec.advanceCount = state.advanceCount();
        spec.latest = new Quote(state.symbol(), state.price(), state.cumulativeVolume(),
                state.lastTimestamp(), state.sequence());
        specs.put(state.symbol(), spec);
    }

    public List<SymbolState> snapshot() {
        List<SymbolState> states = new ArrayList<>();
        for (SymbolSpec spec : specs.values()) {
            synchronized (spec) {
                states.add(new SymbolState(spec.symbol, spec.startPrice, spec.baseVolumePerTick, spec.seed,
                        spec.price, spec.cumulativeVolume, spec.sequence, spec.frozen,
                        spec.latest == null ? 0L : spec.latest.timestamp(), spec.advanceCount));
            }
        }
        return states;
    }

    public void saveTo(Path stateFile, ObjectMapper objectMapper) {
        try {
            Files.createDirectories(stateFile.getParent());
            Files.writeString(stateFile,
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(snapshot()),
                    StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                    java.nio.file.StandardOpenOption.SYNC);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot save market state", e);
        }
    }

    public void restoreFrom(Path stateFile, ObjectMapper objectMapper) {
        try {
            if (Files.notExists(stateFile)) {
                return;
            }
            List<SymbolState> states = objectMapper.readValue(
                    Files.readString(stateFile, StandardCharsets.UTF_8),
                    new TypeReference<List<SymbolState>>() {
                    });
            for (SymbolState state : states) {
                restore(state);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot restore market state", e);
        }
    }

    @Override
    public Quote latestQuote(String symbol) {
        SymbolSpec spec = specs.get(symbol);
        return spec == null ? null : spec.latest;
    }

    @Override
    public Quote advance(String symbol) {
        SymbolSpec spec = specs.get(symbol);
        if (spec == null) {
            return null;
        }
        synchronized (spec) {
            if (!spec.registered) {
                spec.registered = true;
                spec.latest = new Quote(symbol, spec.price, spec.cumulativeVolume,
                        clock.now(), ++spec.sequence);
            } else if (!spec.frozen) {
                long step = ++spec.advanceCount;
                Random rng = new Random(spec.seed ^ (step * 0x9E3779B97F4A7C15L));
                long priceDelta = rng.nextInt(-3, 4);
                spec.price = Math.max(1L, spec.price + priceDelta);
                long tickVolume = Math.max(0L, spec.baseVolumePerTick + rng.nextInt(-50, 51));
                spec.cumulativeVolume += tickVolume;
                spec.latest = new Quote(symbol, spec.price, spec.cumulativeVolume,
                        clock.now(), ++spec.sequence);
            }
            return spec.latest;
        }
    }

    public void setFrozen(String symbol, boolean frozen) {
        SymbolSpec spec = specs.get(symbol);
        if (spec != null) {
            synchronized (spec) {
                spec.frozen = frozen;
            }
        }
    }

    public boolean isFrozen(String symbol) {
        SymbolSpec spec = specs.get(symbol);
        return spec != null && spec.frozen;
    }

    public record SymbolState(String symbol, long startPrice, long baseVolumePerTick, long seed,
                              long price, long cumulativeVolume, long sequence, boolean frozen,
                              long lastTimestamp, long advanceCount) {
    }

    private static final class SymbolSpec {

        private final String symbol;
        private final long startPrice;
        private final long baseVolumePerTick;
        private final long seed;
        private long price;
        private long cumulativeVolume;
        private long sequence;
        private long advanceCount;
        private boolean registered;
        private boolean frozen;
        private Quote latest;

        private SymbolSpec(String symbol, long startPrice, long baseVolumePerTick, long seed) {
            this.symbol = symbol;
            this.startPrice = startPrice;
            this.price = startPrice;
            this.baseVolumePerTick = baseVolumePerTick;
            this.seed = seed;
        }
    }
}
