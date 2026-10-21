package com.eurowings.disruption.mock;

import com.eurowings.disruption.model.Route;
import com.eurowings.disruption.raw.ExternalFlightsRaw;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Simulates the external (non-group) airline search (~500ms) for one route.
 */
@Component
public class ExternalFlightsMock {

    private static final String RESOURCE = "data/external-flights.json";

    private final JsonResourceReader reader;
    private final ExecutorService executor;
    private final long delayMs;

    public ExternalFlightsMock(JsonResourceReader reader,
                               @Qualifier("mockExecutorService") ExecutorService executor,
                               @Value("${mock.external.delay-ms:500}") long delayMs) {
        this.reader = reader;
        this.executor = executor;
        this.delayMs = delayMs;
    }

    public CompletableFuture<ExternalFlightsRaw> search(Route route) {
        return CompletableFuture.supplyAsync(() -> {
            MockLatency.simulate(delayMs);
            ExternalFlightsRaw raw = reader.read(RESOURCE, ExternalFlightsRaw.class);
            raw.setResults(raw.getResults().stream()
                    .filter(r -> route.origin().equals(r.getDepAirport())
                            && route.destination().equals(r.getArrAirport()))
                    .toList());
            return raw;
        }, executor);
    }
}
