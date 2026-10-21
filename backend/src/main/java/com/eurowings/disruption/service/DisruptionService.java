package com.eurowings.disruption.service;

import com.eurowings.disruption.dto.DisruptionOptionsResponse;
import com.eurowings.disruption.dto.DisruptionSummaryDto;
import com.eurowings.disruption.dto.RecommendationDto;
import com.eurowings.disruption.dto.TransportOptionDto;
import com.eurowings.disruption.mock.ExternalFlightsMock;
import com.eurowings.disruption.mock.InternalFlightsMock;
import com.eurowings.disruption.mock.JsonResourceReader;
import com.eurowings.disruption.mock.TrainsMock;
import com.eurowings.disruption.model.Route;
import com.eurowings.disruption.model.Transport;
import com.eurowings.disruption.normalizer.ExternalFlightNormalizer;
import com.eurowings.disruption.normalizer.InternalFlightNormalizer;
import com.eurowings.disruption.normalizer.TrainNormalizer;
import com.eurowings.disruption.raw.DisruptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Orchestrates the disruption use case:
 * derives the affected routes from the bookings, searches all sources per route in parallel,
 * normalizes the results into Transport options, picks one per booking and assembles the API response.
 */
@Service
public class DisruptionService {

    private static final Logger log = LoggerFactory.getLogger(DisruptionService.class);
    private static final String DISRUPTION_RESOURCE = "data/disruption.json";

    private final JsonResourceReader reader;
    private final InternalFlightsMock internalFlightsMock;
    private final ExternalFlightsMock externalFlightsMock;
    private final TrainsMock trainsMock;
    private final InternalFlightNormalizer internalNormalizer;
    private final ExternalFlightNormalizer externalNormalizer;
    private final TrainNormalizer trainNormalizer;
    private final RankingService rankingService;
    private final long sourcesTimeoutMs;

    public DisruptionService(JsonResourceReader reader,
                             InternalFlightsMock internalFlightsMock,
                             ExternalFlightsMock externalFlightsMock,
                             TrainsMock trainsMock,
                             InternalFlightNormalizer internalNormalizer,
                             ExternalFlightNormalizer externalNormalizer,
                             TrainNormalizer trainNormalizer,
                             RankingService rankingService,
                             @Value("${disruption.sources.timeout-ms:3000}") long sourcesTimeoutMs) {
        this.reader = reader;
        this.internalFlightsMock = internalFlightsMock;
        this.externalFlightsMock = externalFlightsMock;
        this.trainsMock = trainsMock;
        this.internalNormalizer = internalNormalizer;
        this.externalNormalizer = externalNormalizer;
        this.trainNormalizer = trainNormalizer;
        this.rankingService = rankingService;
        this.sourcesTimeoutMs = sourcesTimeoutMs;
    }

    public DisruptionOptionsResponse getDisruptionOptions() {
        DisruptionData data = reader.read(DISRUPTION_RESOURCE, DisruptionData.class);
        Set<Route> routes = routesOf(data.getBookings());
        List<Transport> options = fetchAllOptions(routes);

        List<BookingAllocation> allocations = rankingService.rank(data.getBookings(), options);
        Map<String, Integer> suggested = rankingService.suggestedPassengers(allocations);
        Function<Transport, TransportOptionDto> toDto =
                t -> TransportOptionDto.from(t, suggested.getOrDefault(t.getOfferId(), 0));

        // One entry per searched route ("CGN#BER"), options ranked best first; empty if nothing was found
        Map<String, List<TransportOptionDto>> transportOptions = new LinkedHashMap<>();
        routes.forEach(route -> transportOptions.put(route.key(), options.stream()
                .filter(t -> route.origin().equals(t.getOrigin()) && route.destination().equals(t.getDestination()))
                .sorted(RankingService.RANKING)
                .map(toDto)
                .toList()));

        Map<String, RecommendationDto> recommendations = allocations.stream()
                .collect(Collectors.toMap(
                        a -> a.booking().getBookingRef(),
                        a -> new RecommendationDto(
                                a.booking().getPassengers(),
                                a.booking().getJourney(),
                                a.transport().map(toDto).orElse(null)),
                        (a, b) -> {
                            throw new IllegalStateException("Duplicate bookingRef in disruption data");
                        },
                        LinkedHashMap::new));

        return new DisruptionOptionsResponse(
                DisruptionSummaryDto.from(data.getDisruption()),
                transportOptions,
                recommendations);
    }

    /**
     * Distinct final routes of the affected bookings (journey.origin -> journey.destination).
     */
    private static Set<Route> routesOf(List<DisruptionData.Booking> bookings) {
        return bookings.stream()
                .map(b -> new Route(b.getJourney().getOrigin(), b.getJourney().getDestination()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Searches every source for every route concurrently (sources x routes calls).
     * A call that fails or does not answer within the timeout contributes no options
     * instead of failing or blocking the whole response.
     */
    private List<Transport> fetchAllOptions(Set<Route> routes) {
        List<CompletableFuture<List<Transport>>> calls = routes.stream()
                .flatMap(route -> Stream.of(
                        guarded("internal-flights " + route, internalFlightsMock.search(route), internalNormalizer::normalize),
                        guarded("external-flights " + route, externalFlightsMock.search(route), externalNormalizer::normalize),
                        guarded("trains " + route, trainsMock.search(route), trainNormalizer::normalize)))
                .toList();

        return calls.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();
    }

    private <R> CompletableFuture<List<Transport>> guarded(String source,
                                                           CompletableFuture<R> call,
                                                           Function<R, List<Transport>> normalizer) {
        return call.thenApply(normalizer)
                .orTimeout(sourcesTimeoutMs, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> {
                    log.warn("Source '{}' unavailable, continuing without it: {}", source, ex.toString());
                    return List.of();
                });
    }
}
