package com.eurowings.disruption.model.impl;

import com.eurowings.disruption.MockData;
import com.eurowings.disruption.model.Leg;
import com.eurowings.disruption.model.TransportMode;
import com.eurowings.disruption.raw.TrainsRaw;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainTransportTest {

    @Test
    void mapsConnectionUsingTheAirportCodesOfTheQuery() {
        TrainTransport trn2 = train("TRN-2");

        assertThat(trn2.getMode()).isEqualTo(TransportMode.TRAIN);
        assertThat(trn2.getOrigin()).isEqualTo("CGN");
        assertThat(trn2.getDestination()).isEqualTo("BER");
        assertThat(trn2.getAvailableSeats()).isEqualTo(95);
        assertThat(trn2.getAmountInEuro()).isEqualByComparingTo("79.90");
        assertThat(trn2.getLegs()).containsExactly(new Leg("RheinSpree Express", "RX", "CGN", "BER", "1188"));
    }

    @Test
    void readsTimesAsBerlinTimeAcrossMidnight() {
        TrainTransport trn2 = train("TRN-2");

        assertThat(trn2.getDepEstTime()).isEqualTo(OffsetDateTime.parse("2026-07-21T20:55:00+02:00"));
        assertThat(trn2.getArrEstTime()).isEqualTo(OffsetDateTime.parse("2026-07-22T01:32:00+02:00"));
    }

    @Test
    void appliesWinterTimeOutsideDaylightSaving() {
        TrainTransport trn1 = train("TRN-1", c -> c.put("departure", "2026-01-15T19:55"));

        assertThat(trn1.getDepEstTime()).isEqualTo(OffsetDateTime.parse("2026-01-15T19:55:00+01:00"));
    }

    @Test
    void acceptsCurrencyBeforeTheAmount() {
        assertThat(train("TRN-1", c -> c.put("price", "EUR 89.90")).getAmountInEuro()).isEqualByComparingTo("89.90");
    }

    @Test
    void rejectsPricesInAnotherCurrency() {
        assertThatThrownBy(() -> train("TRN-1", c -> c.put("price", "89.90 CHF")))
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    void rejectsDecimalComma() {
        assertThatThrownBy(() -> train("TRN-1", c -> c.put("price", "89,90 EUR")))
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    void rejectsTimesWithSecondsOrOffset() {
        assertThatThrownBy(() -> train("TRN-1", c -> c.put("arrival", "2026-07-22T00:18:00+02:00")))
                .isInstanceOf(DateTimeParseException.class);
    }

    private static TrainTransport train(String id) {
        return train(id, c -> { });
    }

    private static TrainTransport train(String id, Consumer<ObjectNode> change) {
        // Connections of the CGN -> BER query
        TrainsRaw.Connection connection = MockData.record(
                "trains.json", "/queries/0/connections", "id", id, TrainsRaw.Connection.class, change);
        return new TrainTransport(connection, "CGN", "BER");
    }
}
