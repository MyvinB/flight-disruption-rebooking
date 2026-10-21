package com.eurowings.disruption.mock;

import com.eurowings.disruption.model.Route;
import com.eurowings.disruption.raw.DisruptionData;
import com.eurowings.disruption.raw.ExternalFlightsRaw;
import com.eurowings.disruption.raw.InternalFlightsRaw;
import com.eurowings.disruption.raw.TrainsRaw;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockSourcesTest {

    private static final Route CGN_BER = new Route("CGN", "BER");
    private static final Route CGN_DXB = new Route("CGN", "DXB");
    private static final Route PMI_BER = new Route("PMI", "BER");
    private static final Route NOT_SERVED = new Route("FRA", "MUC");

    private final JsonResourceReader reader = new JsonResourceReader(new ObjectMapper());
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    private final InternalFlightsMock internal = new InternalFlightsMock(reader, executor, 0);
    private final ExternalFlightsMock external = new ExternalFlightsMock(reader, executor, 0);
    private final TrainsMock trains = new TrainsMock(reader, executor, 0);

    @AfterEach
    void shutdown() {
        executor.shutdownNow();
    }

    @Test
    void internalSearchReturnsOnlyTheRequestedRoute() {
        assertThat(internalOffers(CGN_BER)).containsExactly("INT-1", "INT-2", "INT-3", "INT-4");
        assertThat(internalOffers(CGN_DXB)).containsExactly("INT-5", "INT-6", "INT-7");
        assertThat(internalOffers(PMI_BER)).containsExactly("INT-8", "INT-9");
    }

    @Test
    void externalSearchMatchesExactAirportsNotNearbyOnes() {
        // EXT-2 flies DUS -> BER and is not offered for CGN -> BER
        assertThat(externalOffers(CGN_BER)).containsExactly("EXT-1");
        assertThat(externalOffers(CGN_DXB)).containsExactly("EXT-3", "EXT-4");
        assertThat(externalOffers(PMI_BER)).containsExactly("EXT-5", "EXT-6");
    }

    @Test
    void trainSearchReturnsConnectionsOnlyWhereRailExists() {
        List<TrainsRaw.Query> cgnBer = trains.search(CGN_BER).join().getQueries();
        assertThat(cgnBer).singleElement()
                .satisfies(q -> assertThat(q.getConnections()).extracting(TrainsRaw.Connection::getId)
                        .containsExactly("TRN-1", "TRN-2", "TRN-3"));

        // The query exists for Dubai, but without connections
        assertThat(trains.search(CGN_DXB).join().getQueries()).singleElement()
                .satisfies(q -> assertThat(q.getConnections()).isEmpty());
    }

    @Test
    void routesNoSourceServesReturnEmptyResults() {
        assertThat(internalOffers(NOT_SERVED)).isEmpty();
        assertThat(externalOffers(NOT_SERVED)).isEmpty();
        assertThat(trains.search(NOT_SERVED).join().getQueries()).isEmpty();
    }

    @Test
    void searchTakesAtLeastTheConfiguredLatency() {
        InternalFlightsMock slow = new InternalFlightsMock(reader, executor, 300);

        long start = System.nanoTime();
        slow.search(CGN_BER).join();

        assertThat((System.nanoTime() - start) / 1_000_000).isGreaterThanOrEqualTo(300);
    }

    @Test
    void searchRunsOnTheGivenExecutorWithoutBlockingTheCaller() {
        InternalFlightsMock slow = new InternalFlightsMock(reader, executor, 500);

        long start = System.nanoTime();
        var future = slow.search(CGN_BER);

        assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(200);
        assertThat(future).isNotDone();
        future.join();
    }

    @Test
    void interruptedLatencyFailsAndKeepsTheInterruptFlag() {
        Thread.currentThread().interrupt();

        assertThatThrownBy(() -> MockLatency.simulate(1000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Mock call interrupted");
        // Thread.interrupted() also clears the flag again for the following tests
        assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    void readerMapsTheDisruptionFile() {
        DisruptionData data = reader.read("data/disruption.json", DisruptionData.class);

        assertThat(data.getDisruption().getFlight()).isEqualTo("EW 4711");
        assertThat(data.getBookings()).hasSize(48);
        assertThat(data.getBookings().stream().mapToInt(DisruptionData.Booking::getPassengers).sum()).isEqualTo(120);
    }

    @Test
    void readerReportsMissingFiles() {
        assertThatThrownBy(() -> reader.read("data/missing.json", DisruptionData.class))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("data/missing.json");
    }

    private List<String> internalOffers(Route route) {
        return internal.search(route).join().getSearches().stream()
                .flatMap(s -> s.getOffers().stream())
                .map(InternalFlightsRaw.Offer::getOfferId)
                .toList();
    }

    private List<String> externalOffers(Route route) {
        return external.search(route).join().getResults().stream()
                .map(ExternalFlightsRaw.Result::getOfferId)
                .toList();
    }
}
