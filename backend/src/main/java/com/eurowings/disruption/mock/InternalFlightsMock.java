package com.eurowings.disruption.mock;

import com.eurowings.disruption.model.Route;
import com.eurowings.disruption.raw.InternalFlightsRaw;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Simulates the Lufthansa Group internal flight search (~200ms) for one route.
 */
@Component
public class InternalFlightsMock {

    private static final String RESOURCE = "data/internal-flights.json";

    private final JsonResourceReader reader;
    private final ExecutorService executor;
    private final long delayMs;

    public InternalFlightsMock(JsonResourceReader reader,
                               @Qualifier("mockExecutorService") ExecutorService executor,
                               @Value("${mock.internal.delay-ms:200}") long delayMs) {
        this.reader = reader;
        this.executor = executor;
        this.delayMs = delayMs;
    }

    public CompletableFuture<InternalFlightsRaw> search(Route route) {
        return CompletableFuture.supplyAsync(() -> {
            MockLatency.simulate(delayMs);
            InternalFlightsRaw raw = reader.read(RESOURCE, InternalFlightsRaw.class);
            raw.setSearches(raw.getSearches().stream()
                    .filter(s -> route.origin().equals(s.getQuery().getOrigin())
                            && route.destination().equals(s.getQuery().getDestination()))
                    .toList());
            return raw;
        }, executor);
    }
}
