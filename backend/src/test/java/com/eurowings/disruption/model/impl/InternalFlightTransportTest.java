package com.eurowings.disruption.model.impl;

import com.eurowings.disruption.MockData;
import com.eurowings.disruption.model.Leg;
import com.eurowings.disruption.model.TransportMode;
import com.eurowings.disruption.raw.InternalFlightsRaw;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InternalFlightTransportTest {

    @Test
    void mapsOfferFieldsAndKeepsTheGivenOffsets() {
        InternalFlightTransport int1 = internal("INT-1");

        assertThat(int1.getMode()).isEqualTo(TransportMode.INTERNAL_FLIGHT);
        assertThat(int1.getOrigin()).isEqualTo("CGN");
        assertThat(int1.getDestination()).isEqualTo("BER");
        assertThat(int1.getAvailableSeats()).isEqualTo(44);
        assertThat(int1.getAmountInEuro()).isEqualByComparingTo("214.00");
        assertThat(int1.getAmountInEuro().scale()).isEqualTo(2);
        assertThat(int1.getDepEstTime()).isEqualTo(OffsetDateTime.parse("2026-07-21T19:40:00+02:00"));
        assertThat(int1.getArrEstTime()).isEqualTo(OffsetDateTime.parse("2026-07-21T22:40:00+02:00"));
    }

    @Test
    void stripsTheCarrierCodeFromEveryLeg() {
        assertThat(internal("INT-1").getLegs()).containsExactly(
                new Leg("Lufthansa", "LH", "CGN", "FRA", "1975"),
                new Leg("Lufthansa", "LH", "FRA", "BER", "186"));
    }

    @Test
    void roundsPricesToCents() {
        assertThat(internal("INT-1", o -> o.with("pricePerSeat").put("amount", 99.999)).getAmountInEuro())
                .isEqualByComparingTo("100.00");
    }

    @Test
    void keepsFlightNumberThatHasNoCarrierPrefix() {
        InternalFlightTransport int3 = internal("INT-3",
                o -> ((ObjectNode) o.withArray("legs").get(0)).put("flightNumber", "4718"));

        assertThat(int3.getLegs().get(0).number()).isEqualTo("4718");
    }

    @Test
    void rejectsTimesWithoutOffset() {
        assertThatThrownBy(() -> internal("INT-1", o -> o.put("departure", "2026-07-21T19:40:00")))
                .isInstanceOf(DateTimeParseException.class);
    }

    @Test
    void rejectsOfferWithoutPrice() {
        assertThatThrownBy(() -> internal("INT-1", o -> o.remove("pricePerSeat")))
                .isInstanceOf(NullPointerException.class);
    }

    private static InternalFlightTransport internal(String offerId) {
        return internal(offerId, o -> { });
    }

    private static InternalFlightTransport internal(String offerId, Consumer<ObjectNode> change) {
        // Offers of the CGN -> BER search
        return new InternalFlightTransport(MockData.record(
                "internal-flights.json", "/searches/0/offers", "offerId", offerId, InternalFlightsRaw.Offer.class, change));
    }
}
