package com.eurowings.disruption.service;

import com.eurowings.disruption.MockData;
import com.eurowings.disruption.model.Leg;
import com.eurowings.disruption.model.Transport;
import com.eurowings.disruption.model.TransportMode;
import com.eurowings.disruption.normalizer.ExternalFlightNormalizer;
import com.eurowings.disruption.normalizer.InternalFlightNormalizer;
import com.eurowings.disruption.normalizer.TrainNormalizer;
import com.eurowings.disruption.raw.DisruptionData;
import com.eurowings.disruption.raw.ExternalFlightsRaw;
import com.eurowings.disruption.raw.InternalFlightsRaw;
import com.eurowings.disruption.raw.TrainsRaw;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RankingServiceTest {

    private final RankingService rankingService = new RankingService();

    @Test
    void prefersTheEarlierArrivalWhenPricesAreEqual() {
        Transport later = option("LATER", TransportMode.INTERNAL_FLIGHT, "CGN", "BER", "2026-07-21T21:40:00+02:00", 10, "189.00");
        Transport earlier = option("EARLIER", TransportMode.EXTERNAL_FLIGHT, "CGN", "BER", "2026-07-21T21:10:00+02:00", 10, "189.00");

        assertThat(rankingService.rank(List.of(booking("A", 1, "CGN", "BER")), List.of(later, earlier)).get(0).transport())
                .contains(earlier);
    }

    @Test
    void picksEarliestArrivalEvenWhenItCostsMore() {
        Transport early = option("EARLY", TransportMode.INTERNAL_FLIGHT, "CGN", "BER", "2026-07-21T21:00:00+02:00", 10, "250.00");
        Transport cheap = option("CHEAP", TransportMode.TRAIN, "CGN", "BER", "2026-07-21T23:00:00+02:00", 10, "69.90");

        List<BookingAllocation> result = rankingService.rank(
                List.of(booking("A", 2, "CGN", "BER")), List.of(cheap, early));

        assertThat(result.get(0).transport()).contains(early);
    }

    @Test
    void prefersTheCheaperOptionWhenArrivalIsTheSame() {
        Transport internal = option("I", TransportMode.INTERNAL_FLIGHT, "CGN", "BER", "2026-07-21T21:00:00+02:00", 10, "214.00");
        Transport train = option("T", TransportMode.TRAIN, "CGN", "BER", "2026-07-21T21:00:00+02:00", 10, "89.90");

        List<BookingAllocation> result = rankingService.rank(
                List.of(booking("A", 2, "CGN", "BER")), List.of(internal, train));

        // Price comes before the source: the cheaper train wins
        assertThat(result.get(0).transport()).contains(train);
    }

    @Test
    void prefersLufthansaGroupThenOtherAirlinesThenTrainsOnSameArrivalAndPrice() {
        Transport train = option("T", TransportMode.TRAIN, "CGN", "BER", "2026-07-21T21:00:00+02:00", 10, "189.00");
        Transport external = option("E", TransportMode.EXTERNAL_FLIGHT, "CGN", "BER", "2026-07-21T21:00:00+02:00", 10, "189.00");
        Transport internal = option("I", TransportMode.INTERNAL_FLIGHT, "CGN", "BER", "2026-07-21T21:00:00+02:00", 10, "189.00");

        assertThat(rankingService.rank(List.of(booking("A", 1, "CGN", "BER")), List.of(train, external, internal))
                .get(0).transport()).contains(internal);
        assertThat(rankingService.rank(List.of(booking("A", 1, "CGN", "BER")), List.of(train, external))
                .get(0).transport()).contains(external);
    }

    @Test
    void comparesPricesByValueNotScale() {
        Transport external = option("E", TransportMode.EXTERNAL_FLIGHT, "CGN", "BER", "2026-07-21T21:00:00+02:00", 10, "189.0");
        Transport internal = option("I", TransportMode.INTERNAL_FLIGHT, "CGN", "BER", "2026-07-21T21:00:00+02:00", 10, "189.00");

        // 189.0 and 189.00 are the same price, so the source decides
        assertThat(rankingService.rank(List.of(booking("X", 1, "CGN", "BER")), List.of(external, internal))
                .get(0).transport()).contains(internal);
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

    @Test
    void partyExactlyFillingTheSeatsFits() {
        Transport exact = option("EXACT", TransportMode.EXTERNAL_FLIGHT, "CGN", "BER", "2026-07-21T21:00:00+02:00", 4);

        assertThat(rankingService.rank(List.of(booking("A", 4, "CGN", "BER")), List.of(exact)).get(0).transport())
                .contains(exact);
    }

    @Test
    void optionsWithoutSeatsAreNeverRecommended() {
        Transport soldOut = option("SOLD_OUT", TransportMode.INTERNAL_FLIGHT, "CGN", "BER", "2026-07-21T20:00:00+02:00", 0);

        assertThat(rankingService.rank(List.of(booking("A", 1, "CGN", "BER")), List.of(soldOut)).get(0).transport())
                .isEmpty();
    }

    @Test
    void noRecommendationWhenThePartyIsLargerThanEveryOption() {
        Transport small = option("SMALL", TransportMode.TRAIN, "CGN", "BER", "2026-07-21T21:00:00+02:00", 3);

        List<BookingAllocation> result = rankingService.rank(List.of(booking("A", 4, "CGN", "BER")), List.of(small));

        assertThat(result).singleElement().satisfies(a -> assertThat(a.transport()).isEmpty());
        assertThat(rankingService.suggestedPassengers(result)).isEmpty();
    }

    @Test
    void noOptionsAtAllGivesEveryBookingAnEmptyRecommendation() {
        List<BookingAllocation> result = rankingService.rank(
                List.of(booking("A", 1, "CGN", "BER"), booking("B", 2, "CGN", "DXB")), List.of());

        assertThat(result).extracting(BookingAllocation::transport).containsOnly(Optional.empty());
    }

    @Test
    void comparesArrivalInstantsAcrossTimeZones() {
        // 06:05 in Dubai is 04:05 in Berlin, so it arrives before 05:00 Berlin time
        Transport dubaiLocal = option("DXB_LOCAL", TransportMode.TRAIN, "CGN", "DXB", "2026-07-22T06:05:00+04:00", 10);
        Transport berlinLocal = option("BER_LOCAL", TransportMode.INTERNAL_FLIGHT, "CGN", "DXB", "2026-07-22T05:00:00+02:00", 10);

        assertThat(rankingService.rank(List.of(booking("A", 1, "CGN", "DXB")), List.of(berlinLocal, dubaiLocal))
                .get(0).transport()).contains(dubaiLocal);
    }

    @Test
    void keepsTheBookingOrder() {
        Transport option = option("O", TransportMode.TRAIN, "CGN", "BER", "2026-07-21T21:00:00+02:00", 10);

        assertThat(rankingService.rank(
                List.of(booking("Z", 1, "CGN", "BER"), booking("A", 1, "CGN", "BER")), List.of(option)))
                .extracting(a -> a.booking().getBookingRef()).containsExactly("Z", "A");
    }

    @Test
    void recommendsTheEarliestOptionPerRouteForTheMockData() {
        List<BookingAllocation> result = rankingService.rank(mockBookings(), mockOptions());

        assertThat(result).hasSize(48).allSatisfy(a -> assertThat(a.transport()).isPresent());
        assertThat(result).allSatisfy(a -> {
            String route = a.booking().getJourney().getOrigin() + "#" + a.booking().getJourney().getDestination();
            String expected = switch (route) {
                case "CGN#BER" -> "EXT-1"; // 21:50, before INT-1 at 22:40
                case "CGN#DXB" -> "EXT-3"; // 06:05 Dubai time, 5 minutes before INT-5
                case "PMI#BER" -> "INT-8"; // 23:05
                default -> throw new AssertionError("Unexpected route " + route);
            };
            assertThat(a.transport().orElseThrow().getOfferId()).isEqualTo(expected);
        });
        assertThat(rankingService.suggestedPassengers(result))
                .containsExactlyInAnyOrderEntriesOf(Map.of("EXT-1", 60, "EXT-3", 30, "INT-8", 30));
    }

    @Test
    void largePartyFallsBackToTheNextOptionWithEnoughSeatsInTheMockData() {
        // EXT-3 has 9 seats, so a party of 10 gets INT-5 (06:10, 20 seats)
        Transport best = rankingService.rank(List.of(booking("BIG", 10, "CGN", "DXB")), mockOptions())
                .get(0).transport().orElseThrow();

        assertThat(best.getOfferId()).isEqualTo("INT-5");
    }

    @Test
    void routeWithoutAnyMockOptionGetsNoRecommendation() {
        assertThat(rankingService.rank(List.of(booking("X", 1, "DUS", "DXB")), mockOptions()).get(0).transport())
                .isEmpty();
    }

    private static List<DisruptionData.Booking> mockBookings() {
        return MockData.read("disruption.json", DisruptionData.class).getBookings();
    }

    private static List<Transport> mockOptions() {
        return Stream.of(
                        new InternalFlightNormalizer().normalize(MockData.read("internal-flights.json", InternalFlightsRaw.class)),
                        new ExternalFlightNormalizer().normalize(MockData.read("external-flights.json", ExternalFlightsRaw.class)),
                        new TrainNormalizer().normalize(MockData.read("trains.json", TrainsRaw.class)))
                .flatMap(List::stream)
                .toList();
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
        return option(id, mode, origin, destination, arrival, seats, "100.00");
    }

    private static Transport option(String id, TransportMode mode, String origin, String destination,
                                    String arrival, int seats, String priceEur) {
        OffsetDateTime arr = OffsetDateTime.parse(arrival);
        BigDecimal price = new BigDecimal(priceEur);
        return new Transport() {
            @Override public String getOfferId() { return id; }
            @Override public TransportMode getMode() { return mode; }
            @Override public BigDecimal getAmountInEuro() { return price; }
            @Override public int getAvailableSeats() { return seats; }
            @Override public OffsetDateTime getDepEstTime() { return arr.minusHours(2); }
            @Override public OffsetDateTime getArrEstTime() { return arr; }
            @Override public String getOrigin() { return origin; }
            @Override public String getDestination() { return destination; }
            @Override public List<Leg> getLegs() { return List.of(); }
        };
    }
}
