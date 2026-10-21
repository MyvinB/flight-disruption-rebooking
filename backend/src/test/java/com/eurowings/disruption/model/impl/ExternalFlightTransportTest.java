package com.eurowings.disruption.model.impl;

import com.eurowings.disruption.MockData;
import com.eurowings.disruption.model.Leg;
import com.eurowings.disruption.model.TransportMode;
import com.eurowings.disruption.raw.ExternalFlightsRaw;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExternalFlightTransportTest {

    @Test
    void convertsCentsToEurosWithTwoDecimals() {
        ExternalFlightTransport ext3 = external("EXT-3");

        assertThat(ext3.getAmountInEuro()).isEqualByComparingTo("424.00");
        assertThat(ext3.getAmountInEuro().scale()).isEqualTo(2);
        assertThat(ext3.getMode()).isEqualTo(TransportMode.EXTERNAL_FLIGHT);
        assertThat(ext3.getAvailableSeats()).isEqualTo(9);
    }

    @Test
    void readsEachTimeInTheLocalZoneOfItsAirport() {
        ExternalFlightTransport ext4 = external("EXT-4");

        // CGN departure in Berlin summer time, DXB arrival in Dubai time
        assertThat(ext4.getDepEstTime()).isEqualTo(OffsetDateTime.parse("2026-07-21T21:05:00+02:00"));
        assertThat(ext4.getArrEstTime()).isEqualTo(OffsetDateTime.parse("2026-07-22T12:40:00+04:00"));
    }

    @Test
    void splitsFlightNumbersIntoOperatorCodeAndNumber() {
        ExternalFlightTransport ext4 = external("EXT-4");

        assertThat(ext4.getOrigin()).isEqualTo("CGN");
        assertThat(ext4.getDestination()).isEqualTo("DXB");
        assertThat(ext4.getLegs()).containsExactly(
                new Leg("Zephyr Jet", "ZJ", "CGN", "ATH", "4102"),
                new Leg("Zephyr Jet", "ZJ", "ATH", "DXB", "660"));
    }

    @Test
    void roundsFractionalCentsHalfUp() {
        ExternalFlightTransport ext1 = external("EXT-1", r -> r.put("price_cents", 18999));

        assertThat(ext1.getAmountInEuro()).isEqualByComparingTo("189.99");
    }

    @Test
    void keepsFlightNumberWithoutOperatorCodeAsIs() {
        ExternalFlightTransport ext1 = external("EXT-1",
                r -> ((ObjectNode) r.withArray("legs").get(0)).put("flight_no", "88"));

        assertThat(ext1.getLegs().get(0)).isEqualTo(new Leg("Nimbus Airways", "", "CGN", "BER", "88"));
    }

    @Test
    void fallsBackToBerlinTimeForUnknownAirports() {
        ExternalFlightTransport ext1 = external("EXT-1", r -> r.put("arr_airport", "XYZ"));

        assertThat(ext1.getArrEstTime()).isEqualTo(OffsetDateTime.parse("2026-07-21T21:50:00+02:00"));
    }

    @Test
    void rejectsTimesInAnotherFormat() {
        assertThatThrownBy(() -> external("EXT-1", r -> r.put("dep_time", "2026-07-21T20:40")))
                .isInstanceOf(DateTimeParseException.class);
    }

    @Test
    void rejectsMissingTimes() {
        assertThatThrownBy(() -> external("EXT-1", r -> r.remove("arr_time")))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsOffersWithoutLegs() {
        assertThatThrownBy(() -> external("EXT-1", r -> r.remove("legs")))
                .isInstanceOf(NullPointerException.class);
    }

    private static ExternalFlightTransport external(String offerId) {
        return external(offerId, r -> { });
    }

    private static ExternalFlightTransport external(String offerId, Consumer<ObjectNode> change) {
        return new ExternalFlightTransport(MockData.record(
                "external-flights.json", "/results", "offer_id", offerId, ExternalFlightsRaw.Result.class, change));
    }
}
