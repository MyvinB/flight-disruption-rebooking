package com.eurowings.disruption.service;

import com.eurowings.disruption.model.Leg;
import com.eurowings.disruption.model.Transport;
import com.eurowings.disruption.model.TransportMode;
import com.eurowings.disruption.raw.DisruptionData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RankingServiceTest {

    private final RankingService rankingService = new RankingService();

    @Test
    void picksEarliestArrivalAndPrefersInternalOnTie() {
        Transport train = option("T", TransportMode.TRAIN, "CGN", "BER", "2026-07-21T21:00:00+02:00", 10);
        Transport internal = option("I", TransportMode.INTERNAL_FLIGHT, "CGN", "BER", "2026-07-21T21:00:00+02:00", 10);
        Transport later = option("L", TransportMode.INTERNAL_FLIGHT, "CGN", "BER", "2026-07-21T23:00:00+02:00", 10);

        List<BookingAllocation> result = rankingService.rank(
                List.of(booking("A", 2, "CGN", "BER")), List.of(later, train, internal));

        assertThat(result.get(0).transport()).contains(internal);
    }

    @Test
    void decidesEachBookingIndependentlyAndCountsSuggestedPassengers() {
        Transport fast = option("FAST", TransportMode.EXTERNAL_FLIGHT, "CGN", "BER", "2026-07-21T21:00:00+02:00", 5);
        Transport slow = option("SLOW", TransportMode.TRAIN, "CGN", "BER", "2026-07-22T01:00:00+02:00", 100);

        List<BookingAllocation> result = rankingService.rank(
                List.of(booking("A", 4, "CGN", "BER"), booking("B", 2, "CGN", "BER"), booking("C", 1, "CGN", "BER")),
                List.of(fast, slow));

        // Earlier recommendations do not use up seats; FAST fits every party on its own
        assertThat(result).extracting(a -> a.transport().orElseThrow().getOfferId())
                .containsExactly("FAST", "FAST", "FAST");
        // Informational count, allowed to exceed FAST's 5 seats
        assertThat(rankingService.suggestedPassengers(result)).containsEntry("FAST", 7);
    }

    @Test
    void skipsOptionsTooSmallForTheParty() {
        Transport small = option("SMALL", TransportMode.INTERNAL_FLIGHT, "CGN", "BER", "2026-07-21T21:00:00+02:00", 3);
        Transport big = option("BIG", TransportMode.TRAIN, "CGN", "BER", "2026-07-22T01:00:00+02:00", 100);

        List<BookingAllocation> result = rankingService.rank(
                List.of(booking("A", 4, "CGN", "BER")), List.of(small, big));

        assertThat(result.get(0).transport()).contains(big);
    }

    @Test
    void matchesOnFinalDestinationNotCancelledSegment() {
        Transport toBer = option("BER", TransportMode.INTERNAL_FLIGHT, "CGN", "BER", "2026-07-21T21:00:00+02:00", 10);
        Transport toDxb = option("DXB", TransportMode.INTERNAL_FLIGHT, "CGN", "DXB", "2026-07-22T06:10:00+04:00", 10);

        List<BookingAllocation> result = rankingService.rank(
                List.of(booking("A", 1, "CGN", "DXB"), booking("B", 1, "PMI", "BER")), List.of(toBer, toDxb));

        assertThat(result.get(0).transport()).contains(toDxb);
        assertThat(result.get(1).transport()).isEmpty();
    }

    private static DisruptionData.Booking booking(String ref, int passengers, String origin, String destination) {
        DisruptionData.Journey journey = new DisruptionData.Journey();
        journey.setOrigin(origin);
        journey.setDestination(destination);
        DisruptionData.Booking booking = new DisruptionData.Booking();
        booking.setBookingRef(ref);
        booking.setPassengers(passengers);
        booking.setJourney(journey);
        return booking;
    }

    private static Transport option(String id, TransportMode mode, String origin, String destination,
                                    String arrival, int seats) {
        OffsetDateTime arr = OffsetDateTime.parse(arrival);
        return new Transport() {
            @Override public String getOfferId() { return id; }
            @Override public TransportMode getMode() { return mode; }
            @Override public BigDecimal getAmountInEuro() { return BigDecimal.TEN; }
            @Override public int getAvailableSeats() { return seats; }
            @Override public OffsetDateTime getDepEstTime() { return arr.minusHours(2); }
            @Override public OffsetDateTime getArrEstTime() { return arr; }
            @Override public String getOrigin() { return origin; }
            @Override public String getDestination() { return destination; }
            @Override public List<Leg> getLegs() { return List.of(); }
        };
    }
}
