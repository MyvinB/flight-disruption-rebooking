package com.eurowings.disruption.mock;

import com.eurowings.disruption.model.Route;
import com.eurowings.disruption.raw.TrainsRaw;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Simulates the RailLink Europe train search (~1500ms, the slowest source) for one route.
 */
@Component
public class TrainsMock {

    private static final String RESOURCE = "data/trains.json";

    private final JsonResourceReader reader;
    private final ExecutorService executor;
    private final long delayMs;

    public TrainsMock(JsonResourceReader reader,
                      @Qualifier("mockExecutorService") ExecutorService executor,
                      @Value("${mock.trains.delay-ms:1500}") long delayMs) {
        this.reader = reader;
        this.executor = executor;
        this.delayMs = delayMs;
    }

    public CompletableFuture<TrainsRaw> search(Route route) {
        return CompletableFuture.supplyAsync(() -> {
            MockLatency.simulate(delayMs);
            TrainsRaw raw = reader.read(RESOURCE, TrainsRaw.class);
            raw.setQueries(raw.getQueries().stream()
                    .filter(q -> route.origin().equals(q.getFromAirport())
                            && route.destination().equals(q.getToAirport()))
                    .toList());
            return raw;
        }, executor);
    }
}
