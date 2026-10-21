package com.eurowings.disruption.service;

import com.eurowings.disruption.MockData;
import com.eurowings.disruption.dto.DisruptionOptionsResponse;
import com.eurowings.disruption.dto.RecommendationDto;
import com.eurowings.disruption.dto.TransportOptionDto;
import com.eurowings.disruption.exception.DisruptionDataUnavailableException;
import com.eurowings.disruption.exception.InvalidDisruptionDataException;
import com.eurowings.disruption.mock.ExternalFlightsMock;
import com.eurowings.disruption.mock.InternalFlightsMock;
import com.eurowings.disruption.mock.JsonResourceReader;
import com.eurowings.disruption.mock.TrainsMock;
import com.eurowings.disruption.model.Route;
import com.eurowings.disruption.normalizer.ExternalFlightNormalizer;
import com.eurowings.disruption.normalizer.InternalFlightNormalizer;
import com.eurowings.disruption.normalizer.TrainNormalizer;
import com.eurowings.disruption.raw.DisruptionData;
import com.eurowings.disruption.raw.ExternalFlightsRaw;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Runs the orchestration on the real mock data with sources that work, fail, hang or return bad data.
 */
class DisruptionServiceTest {

    private static final long TIMEOUT_MS = 300;

    private final ExecutorService executor = Executors.newFixedThreadPool(16);
    private final JsonResourceReader reader = spy(new JsonResourceReader(new ObjectMapper()));

    private InternalFlightsMock internal = new InternalFlightsMock(reader, executor, 0);
    private ExternalFlightsMock external = new ExternalFlightsMock(reader, executor, 0);
    private TrainsMock trains = new TrainsMock(reader, executor, 0);

    @AfterEach
    void shutdown() {
        executor.shutdownNow();
    }

    @Test
    void combinesAllSourcesPerRoute() {
        DisruptionOptionsResponse response = service().getDisruptionOptions();

        assertThat(response.disruption().flight()).isEqualTo("EW 4711");
        assertThat(response.transportOptions()).containsOnlyKeys("CGN#BER", "CGN#DXB", "PMI#BER");
        assertThat(offerIds(response, "CGN#BER"))
                .containsExactly("EXT-1", "INT-1", "INT-2", "TRN-1", "TRN-2", "INT-3", "TRN-3", "INT-4");
        assertThat(offerIds(response, "CGN#DXB")).containsExactly("EXT-3", "INT-5", "EXT-4", "INT-7", "INT-6");
        assertThat(offerIds(response, "PMI#BER")).containsExactly("INT-8", "EXT-5", "EXT-6", "INT-9");
        assertThat(response.recommendations()).hasSize(48);
    }

    @Test
    void keysRecommendationsByBookingRefInFileOrder() {
        DisruptionOptionsResponse response = service().getDisruptionOptions();

        List<String> refsInFile = MockData.read("disruption.json", DisruptionData.class).getBookings().stream()
                .map(DisruptionData.Booking::getBookingRef)
                .toList();
        assertThat(response.recommendations().keySet()).containsExactlyElementsOf(refsInFile);
        RecommendationDto al0sau = response.recommendations().get("AL0SAU");
        assertThat(al0sau.passengers()).isEqualTo(4);
        assertThat(al0sau.journey().getDestination()).isEqualTo("DXB");
        assertThat(al0sau.transport().offerId()).isEqualTo("EXT-3");
    }

    @Test
    void suggestedPassengersAreCountedOnEveryRouteOption() {
        DisruptionOptionsResponse response = service().getDisruptionOptions();

        assertThat(option(response, "CGN#BER", "EXT-1").suggestedPassengers()).isEqualTo(60);
        assertThat(option(response, "CGN#DXB", "EXT-3").suggestedPassengers()).isEqualTo(30);
        assertThat(option(response, "PMI#BER", "INT-8").suggestedPassengers()).isEqualTo(30);
        assertThat(option(response, "CGN#DXB", "INT-5").suggestedPassengers()).isZero();
    }

    @Test
    void failingSourceIsLeftOutAndTheOthersStillAnswer() {
        internal = mock(InternalFlightsMock.class);
        when(internal.search(any())).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("503")));

        DisruptionOptionsResponse response = service().getDisruptionOptions();

        assertThat(offerIds(response, "CGN#BER")).containsExactly("EXT-1", "TRN-1", "TRN-2", "TRN-3");
        assertThat(offerIds(response, "CGN#DXB")).containsExactly("EXT-3", "EXT-4");
        // Without INT-8, Palma passengers get EXT-5 (23:55), which fits every party of up to 8
        assertThat(response.recommendations().get("AJWFFO").transport().offerId()).isEqualTo("EXT-5");
    }

    @Test
    void sourceThatNeverAnswersIsCutOffAtTheTimeout() {
        trains = mock(TrainsMock.class);
        when(trains.search(any())).thenReturn(new CompletableFuture<>());

        long start = System.nanoTime();
        DisruptionOptionsResponse response = service().getDisruptionOptions();
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        assertThat(elapsed).isBetween(Duration.ofMillis(TIMEOUT_MS), Duration.ofMillis(TIMEOUT_MS + 1500));
        assertThat(offerIds(response, "CGN#BER"))
                .containsExactly("EXT-1", "INT-1", "INT-2", "INT-3", "INT-4")
                .noneMatch(id -> id.startsWith("TRN"));
    }

    @Test
    void sourceReturningMalformedDataIsLeftOut() {
        ExternalFlightsRaw broken = MockData.read("external-flights.json", ExternalFlightsRaw.class);
        ExternalFlightsRaw.Result badTime = MockData.record("external-flights.json", "/results", "offer_id", "EXT-1",
                ExternalFlightsRaw.Result.class, r -> r.put("arr_time", "21:50"));
        broken.setResults(List.of(badTime));
        external = mock(ExternalFlightsMock.class);
        when(external.search(any())).thenReturn(CompletableFuture.completedFuture(broken));

        DisruptionOptionsResponse response = service().getDisruptionOptions();

        assertThat(offerIds(response, "CGN#BER")).noneMatch(id -> id.startsWith("EXT")).contains("INT-1", "TRN-1");
        assertThat(response.recommendations().get("D3HRVC").transport().offerId()).isEqualTo("INT-1");
    }

    @Test
    void allSourcesDownStillAnswersWithEmptyOptions() {
        internal = mock(InternalFlightsMock.class);
        external = mock(ExternalFlightsMock.class);
        trains = mock(TrainsMock.class);
        when(internal.search(any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("down")));
        when(external.search(any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("down")));
        when(trains.search(any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("down")));

        DisruptionOptionsResponse response = service().getDisruptionOptions();

        assertThat(response.transportOptions()).containsOnlyKeys("CGN#BER", "CGN#DXB", "PMI#BER")
                .allSatisfy((route, options) -> assertThat(options).isEmpty());
        assertThat(response.recommendations()).hasSize(48)
                .allSatisfy((ref, rec) -> assertThat(rec.transport()).isNull());
    }

    @Test
    void searchesEachRouteOnceInParallel() {
        internal = spy(new InternalFlightsMock(reader, executor, 200));
        external = new ExternalFlightsMock(reader, executor, 500);
        trains = new TrainsMock(reader, executor, 1000);

        long start = System.nanoTime();
        service(3000).getDisruptionOptions();
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        // Run one after another, the 9 searches would take 3 x (200 + 500 + 1000) ms = 5.1 s
        assertThat(elapsed).isLessThan(Duration.ofMillis(2500));
        verify(internal).search(new Route("CGN", "BER"));
        verify(internal).search(new Route("CGN", "DXB"));
        verify(internal).search(new Route("PMI", "BER"));
        verifyNoMoreInteractions(internal);
    }

    @Test
    void rejectsDuplicateBookingReferences() {
        DisruptionData data = mockDisruption();
        data.setBookings(List.of(data.getBookings().get(0), data.getBookings().get(0)));
        serve(data);

        assertThatThrownBy(() -> service().getDisruptionOptions())
                .isInstanceOf(InvalidDisruptionDataException.class)
                .hasMessage("Duplicate bookingRef D3HRVC");
    }

    @Test
    void rejectsBookingWithoutJourney() {
        DisruptionData data = mockDisruption();
        data.getBookings().get(0).setJourney(null);
        serve(data);

        assertThatThrownBy(() -> service().getDisruptionOptions())
                .isInstanceOf(InvalidDisruptionDataException.class)
                .hasMessage("Booking D3HRVC has no reference or journey");
    }

    @Test
    void rejectsBookingWithoutPassengers() {
        DisruptionData data = mockDisruption();
        data.getBookings().get(0).setPassengers(0);
        serve(data);

        assertThatThrownBy(() -> service().getDisruptionOptions())
                .isInstanceOf(InvalidDisruptionDataException.class)
                .hasMessage("Booking D3HRVC has no passengers");
    }

    @Test
    void rejectsDataWithoutBookings() {
        DisruptionData data = mockDisruption();
        data.setBookings(null);
        serve(data);

        assertThatThrownBy(() -> service().getDisruptionOptions())
                .isInstanceOf(InvalidDisruptionDataException.class)
                .hasMessage("Disruption or bookings missing");
    }

    @Test
    void reportsUnreadableDisruptionDataAsUnavailable() {
        doThrow(new UncheckedIOException("connection reset", new IOException()))
                .when(reader).read(eq("data/disruption.json"), eq(DisruptionData.class));

        assertThatThrownBy(() -> service().getDisruptionOptions())
                .isInstanceOf(DisruptionDataUnavailableException.class)
                .hasCauseInstanceOf(UncheckedIOException.class);
    }

    @Test
    void disruptionWithoutBookingsReturnsEmptyResult() {
        DisruptionData data = mockDisruption();
        data.setBookings(List.of());
        serve(data);

        DisruptionOptionsResponse response = service().getDisruptionOptions();

        assertThat(response.transportOptions()).isEmpty();
        assertThat(response.recommendations()).isEmpty();
    }

    @Test
    void bookingWithoutMatchingOptionGetsNullTransport() {
        DisruptionData data = mockDisruption();
        DisruptionData.Booking first = data.getBookings().get(0);
        first.setPassengers(500);
        serve(data);

        DisruptionOptionsResponse response = service().getDisruptionOptions();

        assertThat(response.recommendations().get(first.getBookingRef()).transport()).isNull();
        assertThat(response.recommendations().get(first.getBookingRef()).passengers()).isEqualTo(500);
    }

    private static DisruptionData mockDisruption() {
        return MockData.read("disruption.json", DisruptionData.class);
    }

    private void serve(DisruptionData data) {
        doReturn(data).when(reader).read(eq("data/disruption.json"), eq(DisruptionData.class));
    }

    private DisruptionService service() {
        return service(TIMEOUT_MS);
    }

    private DisruptionService service(long timeoutMs) {
        return new DisruptionService(reader, internal, external, trains,
                new InternalFlightNormalizer(), new ExternalFlightNormalizer(), new TrainNormalizer(),
                new RankingService(), timeoutMs);
    }

    private static List<String> offerIds(DisruptionOptionsResponse response, String route) {
        return response.transportOptions().get(route).stream().map(TransportOptionDto::offerId).toList();
    }

    private static TransportOptionDto option(DisruptionOptionsResponse response, String route, String offerId) {
        return response.transportOptions().get(route).stream()
                .filter(o -> o.offerId().equals(offerId))
                .findFirst()
                .orElseThrow();
    }
}
